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
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.AuthorizeHandler;
import com.corundumstudio.socketio.CancelableScheduler;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.Disconnectable;
import com.corundumstudio.socketio.HeartbeatHandler;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.messages.PacketsMessage;
import com.corundumstudio.socketio.messages.XHRErrorMessage;
import com.corundumstudio.socketio.messages.XHRPostMessage;
import com.corundumstudio.socketio.parser.ErrorAdvice;
import com.corundumstudio.socketio.parser.ErrorReason;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.parser.PacketType;

@Sharable
public class XHRPollingTransport extends SimpleChannelUpstreamHandler implements Disconnectable {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<UUID, XHRPollingClient> sessionId2Client = new ConcurrentHashMap<UUID, XHRPollingClient>();
    private final CancelableScheduler<UUID> disconnectScheduler;

    private final AuthorizeHandler authorizeHandler;
    private final HeartbeatHandler heartbeatHandler;
    private final Disconnectable disconnectable;
    private final Configuration configuration;
    private final String path;

    public XHRPollingTransport(String connectPath, Disconnectable disconnectable, CancelableScheduler<UUID> scheduler,
                                HeartbeatHandler heartbeatHandler, AuthorizeHandler authorizeHandler, Configuration configuration) {
        this.path = connectPath + "xhr-polling/";
        this.authorizeHandler = authorizeHandler;
        this.configuration = configuration;
        this.heartbeatHandler = heartbeatHandler;
        this.disconnectable = disconnectable;
        this.disconnectScheduler = scheduler;
    }

    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;
            QueryStringDecoder queryDecoder = new QueryStringDecoder(req.getUri());

            Channel channel = ctx.getChannel();
            if (queryDecoder.getPath().startsWith(path)) {
                String[] parts = queryDecoder.getPath().split("/");
                if (parts.length > 3) {
                    UUID sessionId = UUID.fromString(parts[4]);

                    scheduleDisconnect(channel, sessionId);

                    if (HttpMethod.POST.equals(req.getMethod())) {
                        onPost(sessionId, channel, req);
                    } else if (HttpMethod.GET.equals(req.getMethod())) {
                        onGet(sessionId, channel, req);
                    }
                    if (queryDecoder.getParameters().containsKey("disconnect")) {
                        XHRPollingClient client = sessionId2Client.get(sessionId);
                        disconnectable.onDisconnect(client);
                    }
                } else {
                    log.warn("Wrong {} method request path: {}, from ip: {}. Channel closed!",
                            new Object[] {req.getMethod(), path, channel.getRemoteAddress()});
                    channel.close();
                }
                return;
            }
        }
        ctx.sendUpstream(e);
    }

	private void scheduleDisconnect(Channel channel, final UUID sessionId) {
		disconnectScheduler.cancel(sessionId);
		ChannelFuture future = channel.getCloseFuture();
		future.addListener(new ChannelFutureListener() {
			@Override
			public void operationComplete(ChannelFuture future) throws Exception {
				disconnectScheduler.schedule(sessionId, new Runnable() {
					@Override
					public void run() {
		                XHRPollingClient client = sessionId2Client.get(sessionId);
		                if (client != null) {
		                	disconnectable.onDisconnect(client);
		                	log.debug("Client: {} disconnected due to connection timeout", sessionId);
		                }
					}
				}, configuration.getCloseTimeout(), TimeUnit.SECONDS);
			}
		});
	}

    private void onPost(UUID sessionId, Channel channel, HttpRequest req) throws IOException {
        XHRPollingClient client = sessionId2Client.get(sessionId);
        if (client == null) {
            log.debug("Client with sessionId: {} was already disconnected. Channel closed!", sessionId);
            channel.close();
            return;
        }

        String origin = req.getHeader(HttpHeaders.Names.ORIGIN);
        channel.write(new XHRPostMessage(origin));
        Channels.fireMessageReceived(channel, new PacketsMessage(client, req.getContent()));
    }

    private void onGet(UUID sessionId, Channel channel, HttpRequest req) {
        if (!authorizeHandler.isSessionAuthorized(sessionId)) {
            sendError(channel, req, sessionId);
            return;
        }
        String origin = req.getHeader(HttpHeaders.Names.ORIGIN);
        XHRPollingClient client = sessionId2Client.get(sessionId);
        if (client == null) {
            client = createClient(origin, channel, sessionId);
        }

        client.update(channel, origin);
    }

    private XHRPollingClient createClient(String origin, Channel channel, UUID sessionId) {
        XHRPollingClient client = new XHRPollingClient(authorizeHandler, sessionId);
        sessionId2Client.put(sessionId, client);
        client.update(channel, origin);

        authorizeHandler.connect(client);
        if (configuration.isHeartbeatsEnabled()) {
            heartbeatHandler.sendHeartbeat(client);
        }
        log.debug("Client for sessionId: {} was created", sessionId);
        return client;
    }

    private void sendError(Channel channel, HttpRequest req, UUID sessionId) {
        log.debug("Client with sessionId: {} was not found! Reconnect error response sended", sessionId);
        Packet packet = new Packet(PacketType.ERROR);
        packet.setReason(ErrorReason.CLIENT_NOT_HANDSHAKEN);
        packet.setAdvice(ErrorAdvice.RECONNECT);
        channel.write(new XHRErrorMessage(packet, req.getHeader(HttpHeaders.Names.ORIGIN)));
    }

    @Override
    public void onDisconnect(SocketIOClient client) {
        if (client instanceof XHRPollingClient) {
            XHRPollingClient xhrClient = (XHRPollingClient) client;
            sessionId2Client.remove(xhrClient.getSessionId());
        }
    }

}
