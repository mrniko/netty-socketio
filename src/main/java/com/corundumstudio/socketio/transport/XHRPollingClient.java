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
import org.jboss.netty.handler.codec.http.HttpHeaders;
import org.jboss.netty.handler.codec.http.HttpRequest;

import com.corundumstudio.socketio.Disconnectable;
import com.corundumstudio.socketio.messages.XHRNewChannelMessage;
import com.corundumstudio.socketio.messages.XHRPacketMessage;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.parser.PacketType;

public class XHRPollingClient extends BaseClient {

    private final Disconnectable disconnectable;

    private String origin;

    public XHRPollingClient(Disconnectable disconnectable, UUID sessionId) {
    	super(sessionId);
        this.disconnectable = disconnectable;
    }

    public void update(Channel channel, HttpRequest req) {
        this.origin = req.getHeader(HttpHeaders.Names.ORIGIN);
        this.channel = channel;
        channel.write(new XHRNewChannelMessage(sessionId, origin));
    }

    public String getOrigin() {
        return origin;
    }

    public ChannelFuture send(Packet packet) {
        return channel.write(new XHRPacketMessage(sessionId, origin, packet));
    }

    public void disconnect() {
        send(new Packet(PacketType.DISCONNECT));
        disconnectable.onDisconnect(this);
    }

}
