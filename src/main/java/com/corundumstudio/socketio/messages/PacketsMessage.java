package com.corundumstudio.socketio.messages;

import org.jboss.netty.buffer.ChannelBuffer;

import com.corundumstudio.socketio.SocketIOClient;

public class PacketsMessage {

	private final SocketIOClient client;
	private final ChannelBuffer content;

	public PacketsMessage(SocketIOClient client, ChannelBuffer content) {
		this.client = client;
		this.content = content;
	}

	public SocketIOClient getClient() {
		return client;
	}

	public ChannelBuffer getContent() {
		return content;
	}

}
