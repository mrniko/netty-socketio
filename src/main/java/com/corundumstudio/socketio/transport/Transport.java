package com.corundumstudio.socketio.transport;

import org.jboss.netty.channel.Channel;

import com.corundumstudio.socketio.SocketIOClient;

public interface Transport {

    SocketIOClient getClient(Channel channel);

}
