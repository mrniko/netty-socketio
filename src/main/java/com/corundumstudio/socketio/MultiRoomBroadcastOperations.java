/**
 * Copyright (c) 2012-2025 Nikita Koksharov
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
package com.corundumstudio.socketio;

import com.corundumstudio.socketio.protocol.Packet;

import java.util.Collection;
import java.util.HashSet;
import java.util.Objects;
import java.util.Set;
import java.util.function.Predicate;

/**
 * author: liangjiaqi
 * date: 2020/8/8 6:02 PM
 */
public class MultiRoomBroadcastOperations implements BroadcastOperations {

    private final Collection<BroadcastOperations> broadcastOperations;

    public MultiRoomBroadcastOperations(Collection<BroadcastOperations> broadcastOperations) {
        this.broadcastOperations = broadcastOperations;
    }

    @Override
    public Collection<SocketIOClient> getClients() {
        Set<SocketIOClient> clients = new HashSet<SocketIOClient>();
        if (this.broadcastOperations == null || this.broadcastOperations.size() == 0) {
            return clients;
        }
        for (BroadcastOperations b : this.broadcastOperations) {
            clients.addAll(b.getClients());
        }
        return clients;
    }

    @Override
    public <T> void send(Packet packet, BroadcastAckCallback<T> ackCallback) {
        if (this.broadcastOperations == null || this.broadcastOperations.size() == 0) {
            return;
        }
        for (BroadcastOperations b : this.broadcastOperations) {
            b.send(packet, ackCallback);
        }
    }

    @Override
    public void sendEvent(String name, SocketIOClient excludedClient, Object... data) {
        Predicate<SocketIOClient> excludePredicate = socketIOClient -> Objects.equals(
                socketIOClient.getSessionId(), excludedClient.getSessionId()
        );
        sendEvent(name, excludePredicate, data);
    }

    @Override
    public void sendEvent(String name, Predicate<SocketIOClient> excludePredicate, Object... data) {
        if (this.broadcastOperations == null || this.broadcastOperations.size() == 0) {
            return;
        }
        for (BroadcastOperations b : this.broadcastOperations) {
            b.sendEvent(name, excludePredicate, data);
        }
    }

    @Override
    public <T> void sendEvent(String name, Object data, BroadcastAckCallback<T> ackCallback) {
        if (this.broadcastOperations == null || this.broadcastOperations.size() == 0) {
            return;
        }
        for (BroadcastOperations b : this.broadcastOperations) {
            b.sendEvent(name, data, ackCallback);
        }
    }

    @Override
    public <T> void sendEvent(String name, Object data, SocketIOClient excludedClient, BroadcastAckCallback<T> ackCallback) {
        Predicate<SocketIOClient> excludePredicate = socketIOClient -> Objects.equals(
                socketIOClient.getSessionId(), excludedClient.getSessionId()
        );
        sendEvent(name, data, excludePredicate, ackCallback);
    }

    @Override
    public <T> void sendEvent(String name, Object data, Predicate<SocketIOClient> excludePredicate, BroadcastAckCallback<T> ackCallback) {
        if (this.broadcastOperations == null || this.broadcastOperations.size() == 0) {
            return;
        }
        for (BroadcastOperations b : this.broadcastOperations) {
            b.sendEvent(name, data, excludePredicate, ackCallback);
        }
    }

    @Override
    public void send(Packet packet) {
        if (this.broadcastOperations == null || this.broadcastOperations.size() == 0) {
            return;
        }
        for (BroadcastOperations b : this.broadcastOperations) {
            b.send(packet);
        }
    }

    @Override
    public void disconnect() {
        if (this.broadcastOperations == null || this.broadcastOperations.size() == 0) {
            return;
        }
        for (BroadcastOperations b : this.broadcastOperations) {
            b.disconnect();
        }
    }

    @Override
    public void sendEvent(String name, Object... data) {
        if (this.broadcastOperations == null || this.broadcastOperations.size() == 0) {
            return;
        }
        for (BroadcastOperations b : this.broadcastOperations) {
            b.sendEvent(name, data);
        }
    }
}
