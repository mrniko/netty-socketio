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
import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.ack.AckManager;
import com.corundumstudio.socketio.namespace.Namespace;
import com.corundumstudio.socketio.namespace.NamespacesHub;
import com.corundumstudio.socketio.protocol.Packet;
import com.corundumstudio.socketio.protocol.PacketType;
import com.corundumstudio.socketio.scheduler.CancelableScheduler;
import com.corundumstudio.socketio.scheduler.SchedulerKey;
import com.corundumstudio.socketio.transport.NamespaceClient;
import com.corundumstudio.socketio.transport.XHRPollingTransport;

public class PacketListener {

    private final NamespacesHub namespacesHub;
    private final AckManager ackManager;
    private final HeartbeatHandler heartbeatHandler;
    private final XHRPollingTransport xhrPollingTransport;
    private final CancelableScheduler scheduler;

    public PacketListener(HeartbeatHandler heartbeatHandler, AckManager ackManager, NamespacesHub namespacesHub, XHRPollingTransport xhrPollingTransport,
            CancelableScheduler scheduler) {
        this.heartbeatHandler = heartbeatHandler;
        this.ackManager = ackManager;
        this.namespacesHub = namespacesHub;
        this.xhrPollingTransport = xhrPollingTransport;
        this.scheduler = scheduler;
    }

    public void onPacket(Packet packet, NamespaceClient client, Transport transport) {
        final AckRequest ackRequest = new AckRequest(packet, client);

        if (packet.isAckRequested()) {
            ackManager.initAckIndex(client.getSessionId(), packet.getAckId());
        }

        switch (packet.getType()) {
        case PING: {
            Packet outPacket = new Packet(PacketType.PONG);
            outPacket.setData(packet.getData());
            // TODO use future
            client.getBaseClient().send(outPacket, transport);

            if ("probe".equals(packet.getData())) {
                client.getBaseClient().send(new Packet(PacketType.NOOP), Transport.POLLING);
            }
            break;
        }

        case UPGRADE: {
            SchedulerKey key = new SchedulerKey(SchedulerKey.Type.UPGRADE_TIMEOUT, client.getSessionId());
            scheduler.cancel(key);
            // TODO setPingTimeout

            client.getBaseClient().upgradeCurrentTransport(transport);
            break;
        }

        case MESSAGE: {
            if (packet.getSubType() == PacketType.CONNECT) {
                Namespace namespace = namespacesHub.get(packet.getNsp());
                namespace.onConnect(client);
                // send connect handshake packet back to client
                client.getBaseClient().send(packet, transport);
            }

            if (packet.getSubType() == PacketType.ACK) {
                ackManager.onAck(client, packet);
            }

            if (packet.getSubType() == PacketType.EVENT) {
                Namespace namespace = namespacesHub.get(packet.getNsp());
                List<Object> args = Collections.emptyList();
                if (packet.getData() != null) {
                    args = packet.getData();
                }
                namespace.onEvent(client, packet.getName(), args, ackRequest);
            }
            break;
        }

        case CLOSE:
            client.onDisconnect();
            break;

        default:
            break;
        }
    }

}
