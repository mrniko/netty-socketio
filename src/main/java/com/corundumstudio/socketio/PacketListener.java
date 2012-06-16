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
package com.corundumstudio.socketio;

import com.corundumstudio.socketio.namespace.Namespace;
import com.corundumstudio.socketio.namespace.NamespacesHub;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.transport.BaseClient;

public class PacketListener {

    private final NamespacesHub namespacesHub;
    private final AckManager ackManager;
    private final HeartbeatHandler heartbeatHandler;

    public PacketListener(HeartbeatHandler heartbeatHandler, AckManager ackManager, NamespacesHub namespacesHub) {
        this.heartbeatHandler = heartbeatHandler;
        this.ackManager = ackManager;
        this.namespacesHub = namespacesHub;
    }

    public void onPacket(Packet packet, SocketIOClient client) {
        switch (packet.getType()) {
        case CONNECT: {
            //namespacesHub.transferClient((BaseClient)client, getEndpoint(packet));
            client.send(packet);
            break;
        }

        case ACK:
            ackManager.onAck(client, packet);
            break;

        case EVENT: {
            Object data = null;
            if (packet.getArgs() != null
                    && !packet.getArgs().isEmpty()) {
                data = packet.getArgs().get(0);
            }
            Namespace namespace = namespacesHub.get(packet.getEndpoint());
            namespace.onEvent(client, packet.getName(), data);
            break;
        }

        case HEARTBEAT:
            heartbeatHandler.onHeartbeat((BaseClient)client);
            break;

        case MESSAGE: {
            Namespace namespace = namespacesHub.get(packet.getEndpoint());
            namespace.onMessage(client, packet.getData().toString());
            break;
        }

        case JSON: {
            Namespace namespace = namespacesHub.get(packet.getEndpoint());
            namespace.onJsonObject(client, packet.getData());
            break;
        }

        case DISCONNECT:
            Namespace namespace = namespacesHub.get(packet.getEndpoint());
            namespace.onDisconnect(client);
            break;
        }
    }

}
