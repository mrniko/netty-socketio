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
import com.corundumstudio.socketio.handler.ClientHead;
import com.corundumstudio.socketio.namespace.NamespacesHub;
import com.corundumstudio.socketio.protocol.JsonSupport;
import com.corundumstudio.socketio.store.StoreFactory;

public abstract class BaseStoreFactory implements StoreFactory {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private Long nodeId = (long) (Math.random() * 1000000);

    protected Long getNodeId() {
        return nodeId;
    }

    @Override
    public void init(final NamespacesHub namespacesHub, final AuthorizeHandler authorizeHandler, JsonSupport jsonSupport) {
        pubSubStore().subscribe(PubSubType.DISCONNECT, new PubSubListener<DisconnectMessage>() {
            @Override
            public void onMessage(DisconnectMessage msg) {
                log.debug("{} sessionId: {}", PubSubType.DISCONNECT, msg.getSessionId());
            }
        }, DisconnectMessage.class);

        pubSubStore().subscribe(PubSubType.CONNECT, new PubSubListener<ConnectMessage>() {
            @Override
            public void onMessage(ConnectMessage msg) {
                authorizeHandler.connect(msg.getSessionId());
                log.debug("{} sessionId: {}", PubSubType.CONNECT, msg.getSessionId());
            }
        }, ConnectMessage.class);

        pubSubStore().subscribe(PubSubType.DISPATCH, new PubSubListener<DispatchMessage>() {
            @Override
            public void onMessage(DispatchMessage msg) {
                String name = msg.getRoom();

                namespacesHub.get(msg.getNamespace()).dispatch(name, msg.getPacket());
                log.debug("{} packet: {}", PubSubType.DISPATCH, msg.getPacket());
            }
        }, DispatchMessage.class);

        pubSubStore().subscribe(PubSubType.JOIN, new PubSubListener<JoinLeaveMessage>() {
            @Override
            public void onMessage(JoinLeaveMessage msg) {
                String name = msg.getRoom();

                namespacesHub.get(msg.getNamespace()).join(name, msg.getSessionId());
                log.debug("{} sessionId: {}", PubSubType.JOIN, msg.getSessionId());
            }
        }, JoinLeaveMessage.class);

        pubSubStore().subscribe(PubSubType.LEAVE, new PubSubListener<JoinLeaveMessage>() {
            @Override
            public void onMessage(JoinLeaveMessage msg) {
                String name = msg.getRoom();

                namespacesHub.get(msg.getNamespace()).leave(name, msg.getSessionId());
                log.debug("{} sessionId: {}", PubSubType.LEAVE, msg.getSessionId());
            }
        }, JoinLeaveMessage.class);
    }

    @Override
    public abstract PubSubStore pubSubStore();

    @Override
    public void onDisconnect(ClientHead client) {
    }

    @Override
    public String toString() {
        return getClass().getSimpleName() + " (distributed session store, distributed publish/subscribe)";
    }

}
