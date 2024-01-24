/**
 * Copyright (c) 2012-2023 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.corundumstudio.socketio.transport;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.listener.ExceptionListener;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.netty.channel.ChannelHandlerContext;
import java.io.IOException;
import java.net.ServerSocket;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpRequest.BodyPublishers;
import java.net.http.HttpResponse;
import java.net.http.HttpResponse.BodyHandlers;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class HttpTransportTest {

  private SocketIOServer server;

  private ObjectMapper mapper = new ObjectMapper();

  private Pattern responseJsonMatcher = Pattern.compile("([0-9]+)(\\{.*\\})?");

  private Pattern multiResponsePattern = Pattern.compile("((?<type>[0-9])(?<id>[0-9]*)(?<body>.+)\\x{1E})*(?<lasttype>[0-9])(?<lastid>[0-9]*)(?<lastbody>.+)");

  private final String packetSeparator = new String(new byte[] { 0x1e });

  private Logger logger = LoggerFactory.getLogger(HttpTransportTest.class);

  @Before
  public void createTestServer() {
    final int port = findFreePort();
    final Configuration config = new Configuration();
    config.setRandomSession(true);
    config.setTransports(Transport.POLLING);
    config.setPort(port);
    config.setExceptionListener(new ExceptionListener() {
      @Override
      public void onEventException(Exception e, List<Object> args, SocketIOClient client) {
        logger.error("eventException", e);
      }

      @Override
      public void onDisconnectException(Exception e, SocketIOClient client) {
        logger.error("disconnectException", e);
      }

      @Override
      public void onConnectException(Exception e, SocketIOClient client) {
        logger.error("connectException", e);
      }

      @Override
      public void onPingException(Exception e, SocketIOClient client) {
        logger.error("pingException", e);
      }

      @Override
      public void onPongException(Exception e, SocketIOClient client) {
        logger.error("pongException", e);
      }

      @Override
      public boolean exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
        return false;
      }

      @Override
      public void onAuthException(Throwable e, SocketIOClient client) {
        logger.error("authException", e);
      }
    });

    final SocketConfig socketConfig = new SocketConfig();
    socketConfig.setReuseAddress(true);
    config.setSocketConfig(socketConfig);

    this.server = new SocketIOServer(config);
    this.server.start();
  }

  @After
  public void cleanupTestServer() {
    this.server.stop();
  }

  private URI createTestServerUri(final String query) throws URISyntaxException {
    return new URI("http", null , "localhost",  server.getConfiguration().getPort(), server.getConfiguration().getContext() + "/",
        query, null);
  }

  private HttpResponse<String> makeSocketIoRequest(final String sessionId, final String bodyForPost)
      throws URISyntaxException, IOException, InterruptedException {
    final URI uri = createTestServerUri("EIO=4&transport=polling&t=Oqd9eWh" + (sessionId == null ? "" : "&sid=" + sessionId));
    HttpClient client = HttpClient.newHttpClient();
    final var builder = HttpRequest.newBuilder()
        .uri(uri);
    if (bodyForPost != null) {
      builder.POST(BodyPublishers.ofString(bodyForPost));
    } else {
      builder.GET();
    }
    return client.send(builder.build(), BodyHandlers.ofString());
  }

  private void postMessage(final String sessionId, final String body)
      throws URISyntaxException, IOException, InterruptedException {
    HttpResponse<String> response = makeSocketIoRequest(sessionId, body);
    final String responseStr = response.body();
    Assert.assertEquals(responseStr, "ok");
  }

  private String[] pollForListOfResponses(final String sessionId)
      throws URISyntaxException, IOException, InterruptedException {
    HttpResponse<String> response = makeSocketIoRequest(sessionId, null);
    final String responseStr = response.body();
    return responseStr.split(packetSeparator);
  }

  private String connectForSessionId(final String sessionId)
      throws URISyntaxException, IOException, InterruptedException {
    final String firstMessage = pollForListOfResponses(sessionId)[0];
    final Matcher jsonMatcher = responseJsonMatcher.matcher(firstMessage);
    Assert.assertTrue(jsonMatcher.find());
    Assert.assertEquals(jsonMatcher.group(1), "0");
    final JsonNode node = mapper.readTree(jsonMatcher.group(2));
    return node.get("sid").asText();
  }

  @Test
  public void testConnect() throws URISyntaxException, IOException, InterruptedException {
    final String sessionId = connectForSessionId(null);
    Assert.assertNotNull(sessionId);
  }

  @Test
  public void testMultipleMessages() throws URISyntaxException, IOException, InterruptedException {
    server.addEventListener("hello", String.class, (client, data, ackSender) ->
        ackSender.sendAckData(data));
    final String sessionId = connectForSessionId(null);
    final ArrayList<String> events = new ArrayList<>();
    events.add("420[\"hello\", \"world\"]");
    events.add("421[\"hello\", \"socketio\"]");
    postMessage(sessionId, events.stream().collect(Collectors.joining(packetSeparator)));
    final String[] responses = pollForListOfResponses(sessionId);
    Assert.assertEquals(responses.length, 2);
  }

  /**
   * Returns a free port number on localhost.
   *
   * Heavily inspired from org.eclipse.jdt.launching.SocketUtil (to avoid a dependency to JDT just because of this).
   * Slightly improved with close() missing in JDT. And throws exception instead of returning -1.
   *
   * @return a free port number on localhost
   * @throws IllegalStateException if unable to find a free port
   */
  private static int findFreePort() {
    ServerSocket socket = null;
    try {
      socket = new ServerSocket(0);
      socket.setReuseAddress(true);
      return socket.getLocalPort();
    } catch (IOException ignored) {
    } finally {
      if (socket != null) {
        try {
          socket.close();
        } catch (IOException ignored) {
        }
      }
    }
    throw new IllegalStateException("Could not find a free TCP/IP port to start embedded SocketIO Server on");
  }

}
