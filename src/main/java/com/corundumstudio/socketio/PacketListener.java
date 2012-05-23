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

import com.corundumstudio.socketio.parser.Packet;

public class PacketListener {

    private final AckManager ackManager;
    private final SocketIOListener socketIOHandler;
    private final HeartbeatHandler heartbeatHandler;
    private final Disconnectable disconnectHandler;

    public PacketListener(SocketIOListener socketIOHandler, Disconnectable disconnectHandler,
            HeartbeatHandler heartbeatHandler, AckManager ackManager) {
        this.disconnectHandler = disconnectHandler;
        this.socketIOHandler = socketIOHandler;
        this.heartbeatHandler = heartbeatHandler;
        this.ackManager = ackManager;
    }

    public void onPacket(Packet packet, SocketIOClient client) {
        switch (packet.getType()) {
        case ACK:
            ackManager.onAck(client, packet);
            break;

        case EVENT:
            Object data = null;
            if (packet.getArgs() != null
                    && !packet.getArgs().isEmpty()) {
                data = packet.getArgs().get(0);
            }
            socketIOHandler.onEvent(client, packet.getName(), data);
            break;

        case HEARTBEAT:
            heartbeatHandler.onHeartbeat(client);
            break;

        case MESSAGE:
            socketIOHandler.onMessage(client, packet.getData().toString());
            break;

        case JSON:
            socketIOHandler.onJsonObject(client, packet.getData());
            break;

        case DISCONNECT:
            disconnectHandler.onDisconnect(client);
            break;
        }
    }

}
