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

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.Channels;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.QueryStringDecoder;
import org.jboss.netty.handler.codec.http.websocketx.CloseWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshaker;
import org.jboss.netty.handler.codec.http.websocketx.WebSocketServerHandshakerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.AuthorizeHandler;
import com.corundumstudio.socketio.Disconnectable;
import com.corundumstudio.socketio.HeartbeatHandler;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.messages.PacketsMessage;

@Sharable
public class WebSocketTransport extends SimpleChannelUpstreamHandler implements Disconnectable {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final Map<UUID, WebSocketClient> sessionId2Client = new ConcurrentHashMap<UUID, WebSocketClient>();
    private final Map<Integer, WebSocketClient> channelId2Client = new ConcurrentHashMap<Integer, WebSocketClient>();

    private final HeartbeatHandler heartbeatHandler;
    private final AuthorizeHandler authorizeHandler;
    private final Disconnectable disconnectable;
    private final String path;


    public WebSocketTransport(String connectPath, Disconnectable disconnectable,
    			AuthorizeHandler authorizeHandler, HeartbeatHandler heartbeatHandler) {
        this.path = connectPath + "websocket";
        this.authorizeHandler = authorizeHandler;
        this.disconnectable = disconnectable;
        this.heartbeatHandler = heartbeatHandler;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e) throws Exception {
        Object msg = e.getMessage();
        if (msg instanceof CloseWebSocketFrame) {
            ctx.getChannel().close();
        } else if (msg instanceof TextWebSocketFrame) {
            TextWebSocketFrame frame = (TextWebSocketFrame) msg;
            receivePackets(ctx, frame.getBinaryData());
        } else if (msg instanceof HttpRequest) {
            HttpRequest req = (HttpRequest) msg;
            handshake(ctx, req);
        } else {
            ctx.sendUpstream(e);
        }
    }

	private void handshake(ChannelHandlerContext ctx, HttpRequest req) {
		QueryStringDecoder queryDecoder = new QueryStringDecoder(req.getUri());
		Channel channel = ctx.getChannel();
		String path = queryDecoder.getPath();
		if (!path.startsWith(this.path)) {
			return;
		}

		String[] parts = path.split("/");
		if (parts.length <= 3) {
			log.warn("Wrong GET request path: {}, from ip: {}. Channel closed!",
					new Object[] {path, channel.getRemoteAddress()});
			channel.close();
			return;
		}

		UUID sessionId = UUID.fromString(parts[4]);

		WebSocketServerHandshakerFactory factory = new WebSocketServerHandshakerFactory(getWebSocketLocation(req), null, false);
		WebSocketServerHandshaker handshaker = factory.newHandshaker(req);
		if (handshaker != null) {
            handshaker.handshake(channel, req);
            connectClient(channel, sessionId);
		} else {
		    factory.sendUnsupportedWebSocketVersionResponse(ctx.getChannel());
		}
	}

	private void receivePackets(ChannelHandlerContext ctx,
			ChannelBuffer channelBuffer) throws IOException {
		WebSocketClient client = channelId2Client.get(ctx.getChannel().getId());
		Channels.fireMessageReceived(ctx.getChannel(), new PacketsMessage(client, channelBuffer));
	}

    private void connectClient(Channel channel, UUID sessionId) {
        if (!authorizeHandler.isSessionAuthorized(sessionId)) {
            log.warn("Unauthorized client with sessionId: {}, from ip: {}. Channel closed!",
                    new Object[] {sessionId, channel.getRemoteAddress()});
            channel.close();
            return;
        }

        WebSocketClient client = new WebSocketClient(channel, disconnectable, sessionId);
        channelId2Client.put(channel.getId(), client);
        sessionId2Client.put(sessionId, client);
        authorizeHandler.connect(client);

        heartbeatHandler.onHeartbeat(client);
    }

    private String getWebSocketLocation(HttpRequest req) {
        return "ws://" + req.getHeader(HttpHeaders.Names.HOST) + path;
    }

    @Override
    public void onDisconnect(SocketIOClient client) {
        if (client instanceof WebSocketClient) {
            WebSocketClient webClient = (WebSocketClient) client;
            sessionId2Client.remove(webClient.getSessionId());
            channelId2Client.remove(webClient.getChannel().getId());
        }
    }

}
