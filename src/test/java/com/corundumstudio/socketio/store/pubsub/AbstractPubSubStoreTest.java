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

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.GenericContainer;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Abstract base class for PubSub store tests
 */
public abstract class AbstractPubSubStoreTest {

    protected PubSubStore publisherStore;  // 用于发布消息的 store
    protected PubSubStore subscriberStore; // 用于订阅消息的 store
    protected GenericContainer<?> container;
    protected Long publisherNodeId = 2L;   // 发布者的 nodeId
    protected Long subscriberNodeId = 1L;  // 订阅者的 nodeId

    @BeforeEach
    public void setUp() throws Exception {
        container = createContainer();
        if (container != null) {
            container.start();
        }
        publisherStore = createPubSubStore(publisherNodeId);
        subscriberStore = createPubSubStore(subscriberNodeId);
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (publisherStore != null) {
            publisherStore.shutdown();
        }
        if (subscriberStore != null) {
            subscriberStore.shutdown();
        }
        if (container != null && container.isRunning()) {
            container.stop();
        }
    }

    /**
     * Create the container for testing
     */
    protected abstract GenericContainer<?> createContainer();

    /**
     * Create the PubSub store instance for testing with specified nodeId
     */
    protected abstract PubSubStore createPubSubStore(Long nodeId) throws Exception;

    @Test
    public void testBasicPublishSubscribe() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TestMessage> receivedMessage = new AtomicReference<>();

        // Subscribe to a topic using subscriber store
        subscriberStore.subscribe(PubSubType.DISPATCH, new PubSubListener<TestMessage>() {
            @Override
            public void onMessage(TestMessage message) {
                // Should receive messages from different nodes
                if (!subscriberNodeId.equals(message.getNodeId())) {
                    receivedMessage.set(message);
                    latch.countDown();
                }
            }
        }, TestMessage.class);

        // Publish a message using publisher store (different nodeId)
        TestMessage testMessage = new TestMessage();
        testMessage.setContent("test content from different node");
        
        publisherStore.publish(PubSubType.DISPATCH, testMessage);

        // Wait for message to be received
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Message should be received within 5 seconds");
        
        TestMessage received = receivedMessage.get();
        assertNotNull(received, "Message should not be null");
        assertEquals("test content from different node", received.getContent());
        assertEquals(publisherNodeId, received.getNodeId());
    }

    @Test
    public void testMessageFiltering() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TestMessage> receivedMessage = new AtomicReference<>();

        // Subscribe to a topic using subscriber store
        subscriberStore.subscribe(PubSubType.DISPATCH, new PubSubListener<TestMessage>() {
            @Override
            public void onMessage(TestMessage message) {
                // Should not receive messages from the same node
                if (!subscriberNodeId.equals(message.getNodeId())) {
                    receivedMessage.set(message);
                    latch.countDown();
                }
            }
        }, TestMessage.class);

        // Publish a message using publisher store (different nodeId)
        TestMessage testMessage = new TestMessage();
        testMessage.setContent("test content from different node");
        
        publisherStore.publish(PubSubType.DISPATCH, testMessage);

        // Wait for message to be received
        assertTrue(latch.await(5, TimeUnit.SECONDS), "Message should be received within 5 seconds");
        
        TestMessage received = receivedMessage.get();
        assertNotNull(received, "Message should not be null");
        assertEquals("test content from different node", received.getContent());
        assertEquals(publisherNodeId, received.getNodeId());
    }

    @Test
    public void testUnsubscribe() throws InterruptedException {
        CountDownLatch latch = new CountDownLatch(1);
        AtomicReference<TestMessage> receivedMessage = new AtomicReference<>();

        // Subscribe to a topic using subscriber store
        subscriberStore.subscribe(PubSubType.DISPATCH, new PubSubListener<TestMessage>() {
            @Override
            public void onMessage(TestMessage message) {
                // Should receive messages from different nodes
                if (!subscriberNodeId.equals(message.getNodeId())) {
                    receivedMessage.set(message);
                    latch.countDown();
                }
            }
        }, TestMessage.class);

        // Unsubscribe immediately
        subscriberStore.unsubscribe(PubSubType.DISPATCH);

        // Publish a message using publisher store (different nodeId)
        TestMessage testMessage = new TestMessage();
        testMessage.setContent("test content");
        
        publisherStore.publish(PubSubType.DISPATCH, testMessage);

        // Message should not be received
        assertFalse(latch.await(2, TimeUnit.SECONDS), "Message should not be received after unsubscribe");
        assertNull(receivedMessage.get(), "No message should be received");
    }

    @Test
    public void testMultipleTopics() throws InterruptedException {
        CountDownLatch dispatchLatch = new CountDownLatch(1);
        CountDownLatch connectLatch = new CountDownLatch(1);
        AtomicReference<TestMessage> dispatchMessage = new AtomicReference<>();
        AtomicReference<TestMessage> connectMessage = new AtomicReference<>();

        // Subscribe to multiple topics using subscriber store
        subscriberStore.subscribe(PubSubType.DISPATCH, new PubSubListener<TestMessage>() {
            @Override
            public void onMessage(TestMessage message) {
                // Should receive messages from different nodes
                if (!subscriberNodeId.equals(message.getNodeId())) {
                    dispatchMessage.set(message);
                    dispatchLatch.countDown();
                }
            }
        }, TestMessage.class);

        subscriberStore.subscribe(PubSubType.CONNECT, new PubSubListener<TestMessage>() {
            @Override
            public void onMessage(TestMessage message) {
                // Should receive messages from different nodes
                if (!subscriberNodeId.equals(message.getNodeId())) {
                    connectMessage.set(message);
                    connectLatch.countDown();
                }
            }
        }, TestMessage.class);

        // Publish messages to different topics using publisher store
        TestMessage dispatchMsg = new TestMessage();
        dispatchMsg.setContent("dispatch message");
        
        TestMessage connectMsg = new TestMessage();
        connectMsg.setContent("connect message");

        publisherStore.publish(PubSubType.DISPATCH, dispatchMsg);
        publisherStore.publish(PubSubType.CONNECT, connectMsg);

        // Wait for both messages
        assertTrue(dispatchLatch.await(5, TimeUnit.SECONDS), "Dispatch message should be received");
        assertTrue(connectLatch.await(5, TimeUnit.SECONDS), "Connect message should be received");

        assertEquals("dispatch message", dispatchMessage.get().getContent());
        assertEquals("connect message", connectMessage.get().getContent());
    }

}
