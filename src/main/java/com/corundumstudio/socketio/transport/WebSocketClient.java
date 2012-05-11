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

import java.util.UUID;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;

import com.corundumstudio.socketio.Disconnectable;
import com.corundumstudio.socketio.messages.WebSocketPacketMessage;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.parser.PacketType;

public class WebSocketClient extends BaseClient {

    private final Disconnectable disconnectable;

    public WebSocketClient(Channel channel, Disconnectable disconnectable, UUID sessionId) {
        super(sessionId);
        this.channel = channel;
        this.disconnectable = disconnectable;
    }

    public Channel getChannel() {
    	return channel;
    }

    public ChannelFuture send(Packet packet) {
        return channel.write(new WebSocketPacketMessage(sessionId, packet));
    }

    public void disconnect() {
        ChannelFuture future = send(new Packet(PacketType.DISCONNECT));
        future.addListener(ChannelFutureListener.CLOSE);

        disconnectable.onDisconnect(this);
    }

}
