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

import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.IOException;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpMethod;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.messages.AuthorizeMessage;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.parser.PacketType;

@Sharable
public class AuthorizeHandler extends SimpleChannelUpstreamHandler implements Disconnectable {

    private final Logger log = LoggerFactory.getLogger(getClass());

    // 'UUID' to 'timestamp' mapping
    // this map will be always smaller than 'connectedSessionIds'
    private final Map<UUID, Long> authorizedSessionIds = new ConcurrentHashMap<UUID, Long>();
    private final Set<UUID> connectedSessionIds = Collections.newSetFromMap(new ConcurrentHashMap<UUID, Boolean>());

    private final String connectPath;

    private final Configuration configuration;
    private final SocketIOListener socketIOListener;

    public AuthorizeHandler(String connectPath, SocketIOListener socketIOListener, Configuration configuration) {
        super();
        this.connectPath = connectPath;
        this.socketIOListener = socketIOListener;
        this.configuration = configuration;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;
            Channel channel = ctx.getChannel();
            QueryStringDecoder queryDecoder = new QueryStringDecoder(req.getUri());
            if (!queryDecoder.getPath().startsWith(connectPath)) {
                HttpResponse res = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.BAD_REQUEST);
                ChannelFuture f = channel.write(res);
                f.addListener(ChannelFutureListener.CLOSE);
            	return;
            }
            if (HttpMethod.GET.equals(req.getMethod()) && queryDecoder.getPath().equals(connectPath)) {
                authorize(channel, req, queryDecoder.getParameters());
                return;
            }
        }
        ctx.sendUpstream(e);
    }

    private void authorize(Channel channel, HttpRequest req, Map<String, List<String>> params)
            throws IOException {
        removeStaleAuthorizedIds();

        final UUID sessionId = UUID.randomUUID();
        authorizedSessionIds.put(sessionId, System.currentTimeMillis());

        String transports = "xhr-polling,websocket";
        //String transports = "websocket";
        String heartbeatTimeoutVal = String.valueOf(configuration.getHeartbeatTimeout());
        if (configuration.getHeartbeatTimeout() == 0) {
            heartbeatTimeoutVal = "";
        }

        String msg = sessionId + ":" + heartbeatTimeoutVal + ":" + configuration.getCloseTimeout() + ":" + transports;

        List<String> jsonpParams = params.get("jsonp");
        String jsonpParam = null;
        if (jsonpParams != null) {
            jsonpParam = jsonpParams.get(0);
        }
        String origin = req.getHeader(HttpHeaders.Names.ORIGIN);
        channel.write(new AuthorizeMessage(msg, jsonpParam, origin, sessionId));
        log.debug("New sessionId: {} authorized", sessionId);
    }

    public boolean isSessionAuthorized(UUID sessionId) {
        return connectedSessionIds.contains(sessionId)
                    || authorizedSessionIds.containsKey(sessionId);
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

    public void connect(SocketIOClient client) {
        authorizedSessionIds.remove(client.getSessionId());
        connectedSessionIds.add(client.getSessionId());

        client.send(new Packet(PacketType.CONNECT));
        socketIOListener.onConnect(client);
    }

    @Override
    public void onDisconnect(SocketIOClient client) {
        connectedSessionIds.remove(client.getSessionId());
    }

}
