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

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.transport.XHRPollingClient;

public class PacketListener {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final SocketIORouter socketIORouter;
    private final HeartbeatHandler heartbeatHandler;
    private final SocketIOListener socketIOHandler;

    public PacketListener(SocketIOListener socketIOHandler, SocketIORouter socketIORouter,
            HeartbeatHandler heartbeatHandler) {
        this.socketIORouter = socketIORouter;
        this.socketIOHandler = socketIOHandler;
        this.heartbeatHandler = heartbeatHandler;
    }

    public void onPacket(Packet packet, XHRPollingClient client) {
        switch (packet.getType()) {
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
            log.debug("Client with sessionId: {} disconnected by client request", client.getSessionId());
            socketIORouter.disconnect(client.getSessionId());
            break;
        }
    }

}
