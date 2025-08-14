package com.corundumstudio.socketio.store.pubsub;

import com.corundumstudio.socketio.store.CustomizedRedisContainer;
import com.corundumstudio.socketio.store.RedissonPubSubStore;
import org.redisson.Redisson;
import org.redisson.api.RedissonClient;
import org.redisson.config.Config;
import org.testcontainers.containers.GenericContainer;

/**
 * Test class for RedissonPubSubStore using testcontainers
 */
public class RedissonPubSubStoreTest extends AbstractPubSubStoreTest {

    private RedissonClient redissonPub;
    private RedissonClient redissonSub;

    @Override
    protected GenericContainer<?> createContainer() {
        return new CustomizedRedisContainer();
    }

    @Override
    protected PubSubStore createPubSubStore(Long nodeId) throws Exception {
        CustomizedRedisContainer customizedRedisContainer = (CustomizedRedisContainer) container;
        Config config = new Config();
        config.useSingleServer()
                .setAddress("redis://" + customizedRedisContainer.getHost() + ":" + customizedRedisContainer.getRedisPort());
        
        redissonPub = Redisson.create(config);
        redissonSub = Redisson.create(config);
        return new RedissonPubSubStore(redissonPub, redissonSub, nodeId);
    }

    @Override
    public void tearDown() throws Exception {
        if (redissonPub != null) {
            redissonPub.shutdown();
        }
        if (redissonSub != null) {
            redissonSub.shutdown();
        }
        if (container != null && container.isRunning()) {
            container.stop();
        }
    }
}
