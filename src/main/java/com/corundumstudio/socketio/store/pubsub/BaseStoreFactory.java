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

import com.corundumstudio.socketio.namespace.NamespacesHub;
import com.corundumstudio.socketio.parser.JsonSupport;
import com.corundumstudio.socketio.store.StoreFactory;
import com.corundumstudio.socketio.transport.MainBaseClient;

public abstract class BaseStoreFactory implements StoreFactory {

    private Long nodeId = (long) (Math.random() * 1000000);

    protected Long getNodeId() {
        return nodeId;
    }

    public void init(final NamespacesHub namespacesHub, JsonSupport jsonSupport) {
        getPubSubStore().subscribe(PubSubStore.DISPATCH, new PubSubListener<DispatchMessage>() {
            @Override
            public void onMessage(DispatchMessage msg) {
                String name = msg.getRoom();

                String[] parts = name.split("/");
                String namespaceName = name;
                if (parts.length > 1) {
                    namespaceName = parts[0];
                }
                namespacesHub.get(namespaceName).dispatch(name, msg.getPacket());
            }
        }, DispatchMessage.class);

        getPubSubStore().subscribe(PubSubStore.JOIN, new PubSubListener<JoinLeaveMessage>() {
            @Override
            public void onMessage(JoinLeaveMessage msg) {
                String name = msg.getRoom();

                String[] parts = name.split("/");
                String namespaceName = name;
                if (parts.length > 1) {
                    namespaceName = parts[0];
                }
                namespacesHub.get(namespaceName).join(name, msg.getSessionId());
            }
        }, JoinLeaveMessage.class);

        getPubSubStore().subscribe(PubSubStore.LEAVE, new PubSubListener<JoinLeaveMessage>() {
            @Override
            public void onMessage(JoinLeaveMessage msg) {
                String name = msg.getRoom();

                String[] parts = name.split("/");
                String namespaceName = name;
                if (parts.length > 1) {
                    namespaceName = parts[0];
                }
                namespacesHub.get(namespaceName).leave(name, msg.getSessionId());
            }
        }, JoinLeaveMessage.class);
    }

    @Override
    public abstract PubSubStore getPubSubStore();

    @Override
    public void onDisconnect(MainBaseClient client) {
    }

}
