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
package com.corundumstudio.socketio.store.pubsub;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.handler.AuthorizeHandler;
import com.corundumstudio.socketio.namespace.NamespacesHub;
import com.corundumstudio.socketio.parser.JsonSupport;
import com.corundumstudio.socketio.store.StoreFactory;
import com.corundumstudio.socketio.transport.MainBaseClient;

public abstract class BaseStoreFactory implements StoreFactory {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Long nodeId = (long) (Math.random() * 1000000);

    protected Long getNodeId() {
        return nodeId;
    }

    public void init(final NamespacesHub namespacesHub, final AuthorizeHandler authorizeHandler, JsonSupport jsonSupport) {
        getPubSubStore().subscribe(PubSubStore.DISCONNECT, new PubSubListener<DisconnectMessage>() {
            @Override
            public void onMessage(DisconnectMessage msg) {
                authorizeHandler.disconnect(msg.getSessionId());
                log.debug("{} sessionId: {}", PubSubStore.DISCONNECT, msg.getSessionId());
            }
        }, DisconnectMessage.class);

        getPubSubStore().subscribe(PubSubStore.CONNECT, new PubSubListener<ConnectMessage>() {
            @Override
            public void onMessage(ConnectMessage msg) {
                authorizeHandler.connect(msg.getSessionId());
                log.debug("{} sessionId: {}", PubSubStore.CONNECT, msg.getSessionId());
            }
        }, ConnectMessage.class);

        getPubSubStore().subscribe(PubSubStore.HANDSHAKE, new PubSubListener<HandshakeMessage>() {
            @Override
            public void onMessage(HandshakeMessage msg) {
                authorizeHandler.handshake(msg.getSessionId(), msg.getData());
                log.debug("{} sessionId: {}", PubSubStore.HANDSHAKE, msg.getSessionId());
            }
        }, HandshakeMessage.class);

        getPubSubStore().subscribe(PubSubStore.DISPATCH, new PubSubListener<DispatchMessage>() {
            @Override
            public void onMessage(DispatchMessage msg) {
                String name = msg.getRoom();

                String namespaceName = extractNamespaceName(name);
                namespacesHub.get(namespaceName).dispatch(name, msg.getPacket());
                log.debug("{} packet: {}", PubSubStore.DISPATCH, msg.getPacket());
            }
        }, DispatchMessage.class);

        getPubSubStore().subscribe(PubSubStore.JOIN, new PubSubListener<JoinLeaveMessage>() {
            @Override
            public void onMessage(JoinLeaveMessage msg) {
                String name = msg.getRoom();

                String namespaceName = extractNamespaceName(name);
                namespacesHub.get(namespaceName).join(name, msg.getSessionId());
                log.debug("{} sessionId: {}", PubSubStore.JOIN, msg.getSessionId());
            }
        }, JoinLeaveMessage.class);

        getPubSubStore().subscribe(PubSubStore.LEAVE, new PubSubListener<JoinLeaveMessage>() {
            @Override
            public void onMessage(JoinLeaveMessage msg) {
                String name = msg.getRoom();

                String namespaceName = extractNamespaceName(name);
                namespacesHub.get(namespaceName).leave(name, msg.getSessionId());
                log.debug("{} sessionId: {}", PubSubStore.LEAVE, msg.getSessionId());
            }
        }, JoinLeaveMessage.class);
    }

    @Override
    public abstract PubSubStore getPubSubStore();

    @Override
    public void onDisconnect(MainBaseClient client) {
    }

    private String extractNamespaceName(String name) {
        String[] parts = name.split("/");
        String namespaceName = name;
        if (parts.length > 1) {
            namespaceName = parts[0];
        }
        return namespaceName;
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (distributed session store, distributed publish/subscribe)";
    }

}
