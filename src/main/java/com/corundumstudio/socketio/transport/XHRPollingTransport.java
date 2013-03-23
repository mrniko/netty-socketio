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
package com.corundumstudio.socketio.transport;

import java.io.IOException;
import java.util.Collection;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.DisconnectableHub;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.ack.AckManager;
import com.corundumstudio.socketio.handler.AuthorizeHandler;
import com.corundumstudio.socketio.messages.PacketsMessage;
import com.corundumstudio.socketio.messages.XHRErrorMessage;
import com.corundumstudio.socketio.messages.XHROutMessage;
import com.corundumstudio.socketio.parser.ErrorAdvice;
import com.corundumstudio.socketio.parser.ErrorReason;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.parser.PacketType;
import com.corundumstudio.socketio.scheduler.CancelableScheduler;
import com.corundumstudio.socketio.scheduler.SchedulerKey;
import com.corundumstudio.socketio.scheduler.SchedulerKey.Type;

@Sharable
public class XHRPollingTransport extends BaseTransport {

    public static final String NAME = "xhr-polling";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<UUID, XHRPollingClient> sessionId2Client =
                                                    new ConcurrentHashMap<UUID, XHRPollingClient>();
    private final CancelableScheduler scheduler;

    private final AckManager ackManager;
    private final AuthorizeHandler authorizeHandler;
    private final DisconnectableHub disconnectable;
    private final Configuration configuration;
    private final String path;

    public XHRPollingTransport(String connectPath, AckManager ackManager, DisconnectableHub disconnectable, CancelableScheduler scheduler,
                                AuthorizeHandler authorizeHandler, Configuration configuration) {
        this.path = connectPath + NAME + "/";
        this.ackManager = ackManager;
        this.authorizeHandler = authorizeHandler;
        this.configuration = configuration;
        this.disconnectable = disconnectable;
        this.scheduler = scheduler;
    }

    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;
            QueryStringDecoder queryDecoder = new QueryStringDecoder(req.getUri());

            Channel channel = ctx.getChannel();
            if (queryDecoder.getPath().startsWith(path)) {
                handleMessage(req, queryDecoder, channel);
                return;
            }
        }
        ctx.sendUpstream(e);
    }

    private void handleMessage(HttpRequest req, QueryStringDecoder queryDecoder, Channel channel)
                                                                                throws IOException {
        String[] parts = queryDecoder.getPath().split("/");
        if (parts.length > 3) {
            UUID sessionId = UUID.fromString(parts[4]);

            String origin = req.getHeader(HttpHeaders.Names.ORIGIN);
            if (queryDecoder.getParameters().containsKey("disconnect")) {
                BaseClient client = sessionId2Client.get(sessionId);
                client.onChannelDisconnect();
                channel.write(new XHROutMessage(origin));
            } else if (HttpMethod.POST.equals(req.getMethod())) {
                onPost(sessionId, channel, origin, req.getContent());
            } else if (HttpMethod.GET.equals(req.getMethod())) {
                onGet(sessionId, channel, origin);
            }
        } else {
            log.warn("Wrong {} method request path: {}, from ip: {}. Channel closed!",
                    new Object[] {req.getMethod(), path, channel.getRemoteAddress()});
            channel.close();
        }
    }

    private void scheduleNoop(Channel channel, final UUID sessionId) {
        SchedulerKey key = new SchedulerKey(Type.POLLING, sessionId);
        scheduler.cancel(key);
        scheduler.schedule(key, new Runnable() {
            @Override
            public void run() {
                XHRPollingClient client = sessionId2Client.get(sessionId);
                if (client != null) {
                    client.send(new Packet(PacketType.NOOP));
                }
            }
        }, configuration.getPollingDuration(), TimeUnit.SECONDS);
    }

    private void scheduleDisconnect(Channel channel, final UUID sessionId) {
        final SchedulerKey key = new SchedulerKey(Type.CLOSE_TIMEOUT, sessionId);
        scheduler.cancel(key);
        ChannelFuture future = channel.getCloseFuture();
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                scheduler.schedule(key, new Runnable() {
                    @Override
                    public void run() {
                        XHRPollingClient client = sessionId2Client.get(sessionId);
                        if (client != null) {
                            client.onChannelDisconnect();
                            log.debug("Client: {} disconnected due to connection timeout", sessionId);
                        }
                    }
                }, configuration.getCloseTimeout(), TimeUnit.SECONDS);
            }
        });
    }

    private void onPost(UUID sessionId, Channel channel, String origin, ChannelBuffer content)
                                                                                throws IOException {
        XHRPollingClient client = sessionId2Client.get(sessionId);
        if (client == null) {
            log.debug("Client with sessionId: {} was already disconnected. Channel closed!", sessionId);
            channel.close();
            return;
        }

        channel.write(new XHROutMessage(origin));
        Channels.fireMessageReceived(channel, new PacketsMessage(client, content));
    }

    private void onGet(UUID sessionId, Channel channel, String origin) {
        if (!authorizeHandler.isSessionAuthorized(sessionId)) {
            sendError(channel, origin, sessionId);
            return;
        }

        XHRPollingClient client = (XHRPollingClient) sessionId2Client.get(sessionId);
        if (client == null) {
            client = createClient(origin, channel, sessionId);
        }

        client.update(channel, origin);

        scheduleDisconnect(channel, sessionId);
        scheduleNoop(channel, sessionId);
    }

    private XHRPollingClient createClient(String origin, Channel channel, UUID sessionId) {
        XHRPollingClient client = new XHRPollingClient(ackManager, disconnectable, sessionId);

        sessionId2Client.put(sessionId, client);
        client.update(channel, origin);

        authorizeHandler.connect(client);
        log.debug("Client for sessionId: {} was created", sessionId);
        return client;
    }

    private void sendError(Channel channel, String origin, UUID sessionId) {
        log.debug("Client with sessionId: {} was not found! Reconnect error response sended", sessionId);
        Packet packet = new Packet(PacketType.ERROR);
        packet.setReason(ErrorReason.CLIENT_NOT_HANDSHAKEN);
        packet.setAdvice(ErrorAdvice.RECONNECT);
        channel.write(new XHRErrorMessage(packet, origin));
    }

    @Override
    public void onDisconnect(BaseClient client) {
        if (client instanceof XHRPollingClient) {
            UUID sessionId = client.getSessionId();

            sessionId2Client.remove(sessionId);
            SchedulerKey noopKey = new SchedulerKey(Type.POLLING, sessionId);
            scheduler.cancel(noopKey);
            SchedulerKey closeTimeoutKey = new SchedulerKey(Type.CLOSE_TIMEOUT, sessionId);
            scheduler.cancel(closeTimeoutKey);
        }
    }

    public Iterable<SocketIOClient> getAllClients() {
        Collection<XHRPollingClient> clients = sessionId2Client.values();
        return getAllClients(clients);
    }

}
