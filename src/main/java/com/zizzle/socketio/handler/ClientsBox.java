/**
 * Copyright (c) 2012-2023 Nikita Koksharov
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
package com.zizzle.socketio.handler;

import io.netty.channel.Channel;
import io.netty.util.internal.PlatformDependent;

import java.util.Map;
import java.util.UUID;

import com.zizzle.socketio.HandshakeData;

public class ClientsBox {

    private final Map<UUID, ClientHead> uuid2clients = PlatformDependent.newConcurrentHashMap();
    private final Map<Channel, ClientHead> channel2clients = PlatformDependent.newConcurrentHashMap();

    // TODO use storeFactory
    public HandshakeData getHandshakeData(UUID sessionId) {
        ClientHead client = uuid2clients.get(sessionId);
        if (client == null) {
            return null;
        }

        return client.getHandshakeData();
    }

    public void addClient(ClientHead clientHead) {
        uuid2clients.put(clientHead.getSessionId(), clientHead);
    }

    public ClientHead removeClient(UUID sessionId) {
        return uuid2clients.remove(sessionId);
    }

    public ClientHead get(UUID sessionId) {
        return uuid2clients.get(sessionId);
    }

    public void add(Channel channel, ClientHead clientHead) {
        channel2clients.put(channel, clientHead);
    }

    public void remove(Channel channel) {
        channel2clients.remove(channel);
    }


    public ClientHead get(Channel channel) {
        return channel2clients.get(channel);
    }

}
