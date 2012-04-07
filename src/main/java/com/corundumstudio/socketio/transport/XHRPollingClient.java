/**
 * Copyright (c) 2012 Nikita Koksharov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package com.corundumstudio.socketio.transport;

import static org.jboss.netty.handler.codec.http.HttpHeaders.Names.*;
import static org.jboss.netty.handler.codec.http.HttpHeaders.Values.*;
import static org.jboss.netty.handler.codec.http.HttpVersion.HTTP_1_1;

import java.io.IOException;
import java.net.SocketAddress;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.jboss.netty.handler.codec.http.DefaultHttpResponse;
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;
import org.jboss.netty.handler.codec.http.HttpResponse;
import org.jboss.netty.handler.codec.http.HttpResponseStatus;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.NullChannelFuture;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIORouter;
import com.corundumstudio.socketio.parser.Encoder;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.parser.PacketType;

public class XHRPollingClient implements SocketIOClient {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private boolean jsonp;
	private final List<String> messages = new LinkedList<String>();
	private final UUID sessionId;

	private String origin;
	private boolean isKeepAlive;
	private boolean connected;
	private Channel channel;
	
	private final SocketIORouter socketIORouter;
	private final Encoder encoder;
	
	public XHRPollingClient(Encoder encoder, SocketIORouter socketIORouter, UUID sessionId) {
		this.encoder = encoder;
		this.socketIORouter = socketIORouter;
		this.sessionId = sessionId;
	}
	
	public UUID getSessionId() {
		return sessionId;
	}
	
    public void doReconnect(Channel channel, HttpRequest req) {
    	this.isKeepAlive = HttpHeaders.isKeepAlive(req);
    	this.origin = req.getHeader(HttpHeaders.Names.ORIGIN);
    	this.channel = channel;
        this.connected = true;
        sendPayload();
    }

    private synchronized ChannelFuture sendPayload() {
        if(!connected || messages.isEmpty()) {
        	return NullChannelFuture.INSTANCE;
        }
    	CharSequence data = encoder.encodePayload(messages);
    	messages.clear();
        return write(data);
    }

    private ChannelFuture write(CharSequence message) {
        HttpResponse res = new DefaultHttpResponse(HTTP_1_1, HttpResponseStatus.OK);

        res.addHeader(CONTENT_TYPE, "text/plain; charset=UTF-8");
        res.addHeader(CONNECTION, KEEP_ALIVE);
        if (origin != null) {
        	res.addHeader("Access-Control-Allow-Origin", origin);
        	res.addHeader("Access-Control-Allow-Credentials", "true");
        }
        if (jsonp) {
        	res.addHeader(CONTENT_TYPE, "application/javascript");
        }

        res.setContent(ChannelBuffers.copiedBuffer(message, CharsetUtil.UTF_8));
        HttpHeaders.setContentLength(res, res.getContent().readableBytes());
        
        connected = false;
        jsonp = false;
        origin = null;

        if(channel.isOpen()) {
        	log.trace("Sending message: {} to client with sessionId: {}", new Object[] {message, sessionId});
            ChannelFuture f = channel.write(res);
            if (!isKeepAlive || res.getStatus().getCode() != 200) {
                f.addListener(ChannelFutureListener.CLOSE);
            }
            return f;
        }
    	return NullChannelFuture.INSTANCE;
    }

    public ChannelFuture sendJsonObject(Object object) {
		Packet packet = new Packet(PacketType.JSON);
		packet.setData(object);
		return send(packet);
    }
    
    public ChannelFuture send(Packet packet) {
		try {
			String message = encoder.encodePacket(packet);
			return sendUnencoded(message);
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
    }
    
    public synchronized ChannelFuture sendUnencoded(String message) {
        messages.add(message);
        return sendPayload();
    }

	public ChannelFuture sendJsonp(String message) {
		jsonp = true;
		return sendUnencoded(message);
	}
	
	public void disconnect() {
		socketIORouter.disconnect(sessionId);
	}

	public SocketAddress getRemoteAddress() {
		return channel.getRemoteAddress();
	}
	
}
