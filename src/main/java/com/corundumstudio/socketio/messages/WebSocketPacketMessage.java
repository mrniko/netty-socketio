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
package com.corundumstudio.socketio.messages;

import io.netty.channel.Channel;

import java.io.IOException;
import java.util.UUID;

import com.corundumstudio.socketio.MessageHandler;
import com.corundumstudio.socketio.parser.Packet;

public class WebSocketPacketMessage extends BaseMessage {

    private final UUID sessionId;
    private final Packet packet;

    public WebSocketPacketMessage(UUID sessionId, Packet packet) {
        super();
        this.sessionId = sessionId;
        this.packet = packet;
    }

    public Packet getPacket() {
        return packet;
    }

    public UUID getSessionId() {
        return sessionId;
    }

    @Override
    public void handleMessage(MessageHandler handler, Channel channel) throws IOException {
        handler.handle(this, channel);
    }

}
