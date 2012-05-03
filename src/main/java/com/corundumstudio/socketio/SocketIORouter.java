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
import java.util.List;
import java.util.Map;
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
import com.corundumstudio.socketio.transport.XHRPollingClient;
import com.corundumstudio.socketio.transport.XHRPollingTransport;

public class SocketIORouter {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private int closeTimeoutSecs = 25;
    private int heartbeatThreadPoolSize;
    private int heartbeatTimeout;
    private int heartbeatInterval;
    private int heartbeatIntervalDiff;

    private final int protocol = 1;
    private final String connectPath = "/socket.io/" + protocol + "/";

    private final ObjectMapper objectMapper;
    private final Decoder decoder;
    private final Encoder encoder;
    private final Set<UUID> authorizedSessionIds = Collections
            .newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());

    private HeartbeatHandler heartbeatHandler;
    private final SocketIOListener socketIOHandler;
    private XHRPollingTransport xhrPollingTransport;

    public SocketIORouter(SocketIOListener socketIOHandler, ObjectMapper objectMapper) {
        this.socketIOHandler = socketIOHandler;
        this.objectMapper = objectMapper;
        encoder = new Encoder(objectMapper);
        decoder = new Decoder(objectMapper);
    }

    public void start() {
        heartbeatHandler = new HeartbeatHandler(heartbeatThreadPoolSize, heartbeatTimeout, heartbeatInterval, heartbeatIntervalDiff);
        PacketListener packetListener = new PacketListener(socketIOHandler, this, heartbeatHandler);
        xhrPollingTransport = new XHRPollingTransport(protocol, decoder, encoder, this, packetListener);
    }

    public void setHeartbeatIntervalDiff(int heartbeatIntervalDiff) {
        this.heartbeatIntervalDiff = heartbeatIntervalDiff;
    }

    /**
     * Heartbeat interval
     *
     * @param value
     *            - time in seconds
     */
    public void setHeartbeatInterval(int heartbeatIntervalSecs) {
        this.heartbeatInterval = heartbeatIntervalSecs;
    }

    /**
     * Heartbeat timeout
     *
     * @param value
     *            - time in seconds
     */
    public void setHeartbeatTimeout(int heartbeatTimeoutSecs) {
        this.heartbeatTimeout = heartbeatTimeoutSecs;
    }

    /**
     * Heartbeat thread pool size
     *
     * @param value
     *            - threads amount
     */
    public void setHeartbeatThreadPoolSize(int heartbeatThreadPoolSize) {
        this.heartbeatThreadPoolSize = heartbeatThreadPoolSize;
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
    }

    public boolean isSessionAuthorized(UUID sessionId) {
        return authorizedSessionIds.contains(sessionId);
    }

    public void connect(SocketIOClient client) {
        heartbeatHandler.cancelHeartbeatCheck(client);

        client.send(new Packet(PacketType.CONNECT));
        heartbeatHandler.sendHeartbeat(client);
        socketIOHandler.onConnect(client);
    }

    private void authorize(Channel channel, HttpRequest msg, Map<String, List<String>> params)
            throws IOException {
        // TODO use common client
        final UUID sessionId = UUID.randomUUID();
        XHRPollingClient client = new XHRPollingClient(encoder, this, null);
        authorizedSessionIds.add(sessionId);

        String transports = "xhr-polling";
        String hs = sessionId + ":" + heartbeatTimeout + ":" + closeTimeoutSecs + ":" + transports;

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
        heartbeatHandler.scheduleHeartbeatCheck(sessionId, new Runnable() {
            public void run() {
                authorizedSessionIds.remove(sessionId);
                log.debug("Authorized sessionId: {} cleared due to connect timeout", sessionId);
            }
        });
    }

    public void disconnect(SocketIOClient client) {
        socketIOHandler.onDisconnect(client);
    }

    public void disconnect(UUID sessionId) {
        authorizedSessionIds.remove(sessionId);
        xhrPollingTransport.disconnect(sessionId);
    }

}
