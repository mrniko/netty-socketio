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
package com.corundumstudio.socketio.handler;

import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.IOException;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.Disconnectable;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.messages.AuthorizeMessage;
import com.corundumstudio.socketio.misc.ConcurrentHashSet;
import com.corundumstudio.socketio.namespace.Namespace;
import com.corundumstudio.socketio.namespace.NamespacesHub;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.parser.PacketType;
import com.corundumstudio.socketio.scheduler.CancelableScheduler;
import com.corundumstudio.socketio.scheduler.SchedulerKey;
import com.corundumstudio.socketio.scheduler.SchedulerKey.Type;
import com.corundumstudio.socketio.transport.BaseClient;

@Sharable
public class AuthorizeHandler extends SimpleChannelUpstreamHandler implements Disconnectable {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final CancelableScheduler disconnectScheduler;
    private final Set<UUID> authorizedSessionIds = new ConcurrentHashSet<UUID>();

    private final String connectPath;
    private final Configuration configuration;
    private final NamespacesHub namespacesHub;

    public AuthorizeHandler(String connectPath, CancelableScheduler scheduler, Configuration configuration, NamespacesHub namespacesHub) {
        super();
        this.connectPath = connectPath;
        this.configuration = configuration;
        this.disconnectScheduler = scheduler;
        this.namespacesHub = namespacesHub;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;
            Channel channel = ctx.getChannel();
            QueryStringDecoder queryDecoder = new QueryStringDecoder(req.getUri());
            if (!configuration.isAllowCustomRequests()
                    && !queryDecoder.getPath().startsWith(connectPath)) {
                HttpResponse res = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
                ChannelFuture f = channel.write(res);
                f.addListener(ChannelFutureListener.CLOSE);
                return;
            }
            if (queryDecoder.getPath().equals(connectPath)) {
                String origin = req.getHeader(HttpHeaders.Names.ORIGIN);
                authorize(channel, origin, queryDecoder.getParameters());
                return;
            }
        }
        ctx.sendUpstream(e);
    }

    private void authorize(Channel channel, String origin, Map<String, List<String>> params)
            throws IOException {
        final UUID sessionId = UUID.randomUUID();
        authorizedSessionIds.add(sessionId);

        scheduleDisconnect(channel, sessionId);

        String heartbeatTimeoutVal = String.valueOf(configuration.getHeartbeatTimeout());
        if (!configuration.isHeartbeatsEnabled()) {
            heartbeatTimeoutVal = "";
        }

        String msg = sessionId + ":" + heartbeatTimeoutVal + ":" + configuration.getCloseTimeout() + ":" + configuration.getTransports();

        List<String> jsonpParams = params.get("jsonp");
        String jsonpParam = null;
        if (jsonpParams != null) {
            jsonpParam = jsonpParams.get(0);
        }

        channel.write(new AuthorizeMessage(msg, jsonpParam, origin, sessionId));
        log.debug("New sessionId: {} authorized", sessionId);
    }

    private void scheduleDisconnect(Channel channel, final UUID sessionId) {
        ChannelFuture future = channel.getCloseFuture();
        future.addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(ChannelFuture future) throws Exception {
                SchedulerKey key = new SchedulerKey(Type.AUTHORIZE, sessionId);
                disconnectScheduler.schedule(key, new Runnable() {
                    @Override
                    public void run() {
                        authorizedSessionIds.remove(sessionId);
                        log.debug("Authorized sessionId: {} removed due to connection timeout", sessionId);
                    }
                }, configuration.getCloseTimeout(), TimeUnit.SECONDS);
            }
        });
    }

    public boolean isSessionAuthorized(UUID sessionId) {
        return authorizedSessionIds.contains(sessionId);
    }

    public void connect(BaseClient client) {
        SchedulerKey key = new SchedulerKey(Type.AUTHORIZE, client.getSessionId());
        disconnectScheduler.cancel(key);
        client.send(new Packet(PacketType.CONNECT));

        Namespace ns = namespacesHub.get(Namespace.DEFAULT_NAME);
        SocketIOClient nsClient = client.getChildClient(ns);
        namespacesHub.get(ns.getName()).onConnect(nsClient);
    }

    @Override
    public void onDisconnect(BaseClient client) {
        authorizedSessionIds.remove(client.getSessionId());
    }

}
