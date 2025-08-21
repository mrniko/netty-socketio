package com.corundumstudio.socketio.store.pubsub;

import org.testcontainers.containers.GenericContainer;

import com.corundumstudio.socketio.store.CustomizedHazelcastContainer;
import com.corundumstudio.socketio.store.HazelcastPubSubStore;
import com.hazelcast.client.HazelcastClient;
import com.hazelcast.client.config.ClientConfig;
import com.hazelcast.core.HazelcastInstance;

/** Test class for HazelcastPubSubStore using testcontainers */
public class HazelcastPubSubStoreTest extends AbstractPubSubStoreTest {

  private HazelcastInstance hazelcastPub;
  private HazelcastInstance hazelcastSub;

  @Override
  protected GenericContainer<?> createContainer() {
    return new CustomizedHazelcastContainer();
  }

  @Override
  protected PubSubStore createPubSubStore(Long nodeId) throws Exception {
    CustomizedHazelcastContainer customizedHazelcastContainer =
        (CustomizedHazelcastContainer) container;
    ClientConfig clientConfig = new ClientConfig();
    clientConfig.getGroupConfig().setName("dev").setPassword("dev-pass");
    clientConfig
        .getNetworkConfig()
        .addAddress(
            customizedHazelcastContainer.getHost()
                + ":"
                + customizedHazelcastContainer.getHazelcastPort());

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
