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
package com.corundumstudio.socketio.handler;

import java.util.Collections;
import java.util.List;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.ack.AckManager;
import com.corundumstudio.socketio.namespace.Namespace;
import com.corundumstudio.socketio.namespace.NamespacesHub;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.transport.NamespaceClient;

public class PacketListener {

    private final NamespacesHub namespacesHub;
    private final AckManager ackManager;
    private final HeartbeatHandler heartbeatHandler;

    public PacketListener(HeartbeatHandler heartbeatHandler, AckManager ackManager, NamespacesHub namespacesHub) {
        this.heartbeatHandler = heartbeatHandler;
        this.ackManager = ackManager;
        this.namespacesHub = namespacesHub;
    }

    public void onPacket(Packet packet, NamespaceClient client) {
        final AckRequest ackRequest = new AckRequest(packet, client);

        if (packet.isAckRequested()) {
            ackManager.initAckIndex(client.getSessionId(), packet.getId());
        }

        switch (packet.getType()) {
        case CONNECT: {
            Namespace namespace = namespacesHub.get(packet.getEndpoint());
            namespace.onConnect(client);
            // send connect handshake packet back to client
            client.send(packet);
            break;
        }

        case PING: {
            break;
        }

        case ACK:
            ackManager.onAck(client, packet);
            break;

        case EVENT: {
            Namespace namespace = namespacesHub.get(packet.getEndpoint());
            List<Object> args = Collections.emptyList();
            if (packet.getData() != null) {
                args = (List<Object>) packet.getData();
            }
            namespace.onEvent(client, packet.getName(), args, ackRequest);
            break;
        }

        case DISCONNECT:
            client.onDisconnect();
            break;

        default:
            break;
        }
    }

}
