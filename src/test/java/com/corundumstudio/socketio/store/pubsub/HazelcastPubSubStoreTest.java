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
package com.corundumstudio.socketio.store.pubsub;

import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;
import com.corundumstudio.socketio.store.CustomizedHazelcastContainer;
import com.corundumstudio.socketio.store.HazelcastPubSubStore;
import org.testcontainers.containers.GenericContainer;

/**
 * Test class for HazelcastPubSubStore using testcontainers
 */
public class HazelcastPubSubStoreTest extends AbstractPubSubStoreTest {

    private HazelcastInstance hazelcastPub;
    private HazelcastInstance hazelcastSub;

    @Override
    protected GenericContainer<?> createContainer() {
        return new CustomizedHazelcastContainer();
    }

    @Override
    protected PubSubStore createPubSubStore(Long nodeId) throws Exception {
        CustomizedHazelcastContainer customizedHazelcastContainer = (CustomizedHazelcastContainer) container;
        ClientConfig clientConfig = new ClientConfig();
        clientConfig.getGroupConfig().setName("dev").setPassword("dev-pass");
        clientConfig.getNetworkConfig().addAddress(
            customizedHazelcastContainer.getHost() + ":" + customizedHazelcastContainer.getHazelcastPort()
        );
        
        hazelcastPub = HazelcastClient.newHazelcastClient(clientConfig);
        hazelcastSub = HazelcastClient.newHazelcastClient(clientConfig);
        
        return new HazelcastPubSubStore(hazelcastPub, hazelcastSub, nodeId);
    }

    @Override
    public void tearDown() throws Exception {
        if (hazelcastPub != null) {
            hazelcastPub.shutdown();
        }
        if (hazelcastSub != null) {
            hazelcastSub.shutdown();
        }
        if (container != null && container.isRunning()) {
            container.stop();
        }
    }
}
