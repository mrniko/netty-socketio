package com.corundumstudio.socketio.transport;

import java.net.SocketAddress;
import java.util.UUID;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.parser.PacketType;

abstract class BaseClient implements SocketIOClient {

	protected final UUID sessionId;
	protected Channel channel;

	public BaseClient(UUID sessionId) {
		this.sessionId = sessionId;
	}

	@Override
	public UUID getSessionId() {
		return sessionId;
	}

	@Override
    public ChannelFuture sendJsonObject(Object object) {
        Packet packet = new Packet(PacketType.JSON);
        packet.setData(object);
        return send(packet);
    }

	@Override
	public SocketAddress getRemoteAddress() {
		return channel.getRemoteAddress();
	}

}
