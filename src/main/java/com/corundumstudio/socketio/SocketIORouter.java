/**
 * Copyright 2012 Nikita Koksharov
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
package com.corundumstudio.socketio;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.parser.Decoder;
import com.corundumstudio.socketio.parser.Encoder;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.parser.PacketType;
import com.corundumstudio.socketio.transport.SocketIOTransport;
import com.corundumstudio.socketio.transport.WebSocketClient;
import com.corundumstudio.socketio.transport.WebSocketTransport;
import com.corundumstudio.socketio.transport.XHRPollingClient;
import com.corundumstudio.socketio.transport.XHRPollingTransport;

public class SocketIORouter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // 'UUID' to 'timestamp' mapping
    // this map will be always smaller than 'connectedSessionIds'
    private final Map<UUID, Long> authorizedSessionIds = new ConcurrentHashMap<UUID, Long>();
    private final Set<UUID> connectedSessionIds = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());

    private final int protocol = 1;
    private final String connectPath = "/socket.io/" + protocol + "/";

    private final ObjectMapper objectMapper;
    private final Decoder decoder;
    private final Encoder encoder;

    private final Configuration configuration;
    private final SocketIOListener socketIOHandler;

    private HeartbeatHandler heartbeatHandler;

    private SocketIOTransport xhrPollingTransport;
    private SocketIOTransport webSocketTransport;

    public SocketIORouter(Configuration configuration) {
        this.configuration = configuration;
        this.socketIOHandler = configuration.getListener();
        this.objectMapper = configuration.getObjectMapper();
        encoder = new Encoder(objectMapper);
        decoder = new Decoder(objectMapper);
    }

    public void start() {
        heartbeatHandler = new HeartbeatHandler(configuration);
        PacketListener packetListener = new PacketListener(socketIOHandler, this, heartbeatHandler);
        xhrPollingTransport = new XHRPollingTransport(connectPath, decoder, encoder, this, packetListener);
        webSocketTransport = new WebSocketTransport(connectPath, decoder, encoder, this, packetListener);
    }

    public void stop() {
        heartbeatHandler.shutdown();
    }

    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;
            Channel channel = ctx.getChannel();
            QueryStringDecoder queryDecoder = new QueryStringDecoder(req.getUri());
            if (HttpMethod.GET.equals(req.getMethod()) && queryDecoder.getPath().equals(connectPath)) {
                authorize(channel, req, queryDecoder.getParameters());
                return;
            }
        }
        xhrPollingTransport.messageReceived(ctx, e);
        webSocketTransport.messageReceived(ctx, e);
    }

    public boolean isSessionAuthorized(UUID sessionId) {
        return connectedSessionIds.contains(sessionId)
                    || authorizedSessionIds.containsKey(sessionId);
    }

    public void connect(SocketIOClient client) {
        authorizedSessionIds.remove(client.getSessionId());
        connectedSessionIds.add(client.getSessionId());

        client.send(new Packet(PacketType.CONNECT));
        if (configuration.isHeartbeatsEnabled()
                && !(client instanceof WebSocketClient)) {
            heartbeatHandler.sendHeartbeat(client);
        }
        socketIOHandler.onConnect(client);
    }

    private void authorize(Channel channel, HttpRequest msg, Map<String, List<String>> params)
            throws IOException {
        removeStaleAuthorizedIds();
        // TODO use common client
        final UUID sessionId = UUID.randomUUID();
        XHRPollingClient client = new XHRPollingClient(encoder, this, null);
        authorizedSessionIds.put(sessionId, System.currentTimeMillis());

        String transports = "xhr-polling,websocket";
        String heartbeatTimeoutVal = String.valueOf(configuration.getHeartbeatTimeout());
        if (configuration.getHeartbeatTimeout() == 0) {
            heartbeatTimeoutVal = "";
        }

        String hs = sessionId + ":" + heartbeatTimeoutVal + ":" + configuration.getCloseTimeout() + ":" + transports;

        List<String> jsonpParam = params.get("jsonp");
        if (jsonpParam != null) {
            String jsonpResponse = "io.j[" + jsonpParam.get(0) + "](" + objectMapper.writeValueAsString(hs)
                    + ");";
            client.sendJsonp(jsonpResponse);
        } else {
            client.sendUnencoded(hs);
        }
        client.doReconnect(channel, msg);
        log.debug("New sessionId: {} authorized", sessionId);
    }

    /**
     * Remove stale authorized client ids which
     * has not connected during some timeout
     */
    private void removeStaleAuthorizedIds() {
        for (Iterator<Entry<UUID, Long>> iterator = authorizedSessionIds.entrySet().iterator(); iterator.hasNext();) {
            Entry<UUID, Long> entry = iterator.next();
            if (System.currentTimeMillis() - entry.getValue() > 60*1000) {
                iterator.remove();
                log.debug("Authorized sessionId: {} cleared due to connection timeout", entry.getKey());
            }
        }
    }

    public void onDisconnect(SocketIOClient client) {
        log.debug("Client with sessionId: {} disconnected by client request", client.getSessionId());
        xhrPollingTransport.onDisconnect(client);
        webSocketTransport.onDisconnect(client);
        disconnect(client);
    }

    public void disconnect(SocketIOClient client) {
        socketIOHandler.onDisconnect(client);
    }

    public void disconnect(UUID sessionId) {
        connectedSessionIds.remove(sessionId);
        xhrPollingTransport.disconnect(sessionId);
        webSocketTransport.disconnect(sessionId);
    }

}
