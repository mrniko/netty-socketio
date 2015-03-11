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

import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.util.AttributeKey;
import io.netty.util.internal.PlatformDependent;

import java.util.Queue;
import java.util.UUID;

import com.corundumstudio.socketio.DisconnectableHub;
import com.corundumstudio.socketio.HandshakeData;
import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.ack.AckManager;
import com.corundumstudio.socketio.messages.XHRSendPacketsMessage;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.store.StoreFactory;

public class XHRPollingClient extends MainBaseClient {

    public static final AttributeKey<Boolean> WRITE_ONCE = AttributeKey.<Boolean>valueOf("writeOnce");

    private final Queue<Packet> packetQueue = PlatformDependent.newMpscQueue();
    private String origin;

    public XHRPollingClient(AckManager ackManager, DisconnectableHub disconnectable,
            UUID sessionId, Transport transport, StoreFactory storeFactory, HandshakeData handshakeData) {
        super(sessionId, ackManager, disconnectable, transport, storeFactory, handshakeData);
    }

    public void bindChannel(Channel channel, String origin) {
        this.origin = origin;
        setChannel(channel);
        channel.writeAndFlush(new XHRSendPacketsMessage(getSessionId(), origin, packetQueue));
    }

    public String getOrigin() {
        return origin;
    }

    public ChannelFuture send(Packet packet) {
        packetQueue.add(packet);
        if (getChannel().attr(WRITE_ONCE).get() == null) {
            return getChannel().writeAndFlush(new XHRSendPacketsMessage(getSessionId(), origin, packetQueue));
        }
        return getChannel().newSucceededFuture();
    }

}
