/**
 * Copyright (c) 2012-2023 Nikita Koksharov
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
package com.corundumstudio.socketio.store;

import com.corundumstudio.socketio.store.pubsub.PubSubListener;
import com.corundumstudio.socketio.store.pubsub.PubSubMessage;
import com.corundumstudio.socketio.store.pubsub.PubSubStore;
import com.corundumstudio.socketio.store.pubsub.PubSubType;
import com.hazelcast.core.HazelcastInstance;
import io.netty.util.internal.PlatformDependent;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;
import java.util.Objects;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;
import java.util.function.Consumer;

/**
 * Hazelcast PubSubStore implementation compatible with Hazelcast 3.x, 4.x, and 5.x.
 * <ul>
 *   <li>Hazelcast 3.x: com.hazelcast.core.{ITopic, Message, MessageListener}, registration ID is String</li>
 *   <li>Hazelcast 4.x/5.x: com.hazelcast.topic.{ITopic, Message, MessageListener}, registration ID is UUID</li>
 * </ul>
 */
public class HazelcastPubSubStore implements PubSubStore {

    private final HazelcastInstance hazelcastPub;
    private final HazelcastInstance hazelcastSub;
    private final Long nodeId;
    private final ConcurrentMap<String, Queue<String>> listenerRegistrations = PlatformDependent.newConcurrentHashMap();

    public HazelcastPubSubStore(HazelcastInstance hazelcastPub, HazelcastInstance hazelcastSub, Long nodeId) {
        this.hazelcastPub = Objects.requireNonNull(hazelcastPub, "hazelcastPub must not be null");
        this.hazelcastSub = Objects.requireNonNull(hazelcastSub, "hazelcastSub must not be null");
        this.nodeId = Objects.requireNonNull(nodeId, "nodeId must not be null");
    }

    @Override
    public void publish(PubSubType type, PubSubMessage message) {
        message.setNodeId(nodeId);
        Object topic = HazelcastMethodHandles.getTopic(hazelcastPub, type.toString());
        HazelcastMethodHandles.publish(topic, message);
    }

    @Override
    public <T extends PubSubMessage> void subscribe(PubSubType type, PubSubListener<T> listener, Class<T> clazz) {
        String topicName = type.toString();
        Object topic = HazelcastMethodHandles.getTopic(hazelcastSub, topicName);

        Object hazelcastListener = HazelcastMethodHandles.createMessageListener(
                new FilteredMessageHandler<>(nodeId, listener)
        );

        String registrationId = HazelcastMethodHandles.addMessageListener(topic, hazelcastListener);

        listenerRegistrations
                .computeIfAbsent(topicName, k -> new ConcurrentLinkedQueue<>())
                .add(registrationId);
    }

    @Override
    public void unsubscribe(PubSubType type) {
        Objects.requireNonNull(type, "type must not be null");

        String topicName = type.toString();
        Queue<String> registrationIds = listenerRegistrations.remove(topicName);

        if (registrationIds == null || registrationIds.isEmpty()) {
            return;
        }

        Object topic = HazelcastMethodHandles.getTopic(hazelcastSub, topicName);
        for (String registrationId : registrationIds) {
            HazelcastMethodHandles.removeMessageListener(topic, registrationId);
        }
    }

    @Override
    public void shutdown() {
    }

    /**
     * Message handler that filters out messages from the same node.
     */
    private static final class FilteredMessageHandler<T extends PubSubMessage> implements Consumer<T> {

        private final Long localNodeId;
        private final PubSubListener<T> delegate;

        FilteredMessageHandler(Long localNodeId, PubSubListener<T> delegate) {
            this.localNodeId = localNodeId;
            this.delegate = delegate;
        }

        @Override
        public void accept(T message) {
            if (!localNodeId.equals(message.getNodeId())) {
                delegate.onMessage(message);
            }
        }
    }

    static final class HazelcastMethodHandles {

        private static final String HAZELCAST_4_MESSAGE_LISTENER = "com.hazelcast.topic.MessageListener";
        private static final String HAZELCAST_4_MESSAGE = "com.hazelcast.topic.Message";
        private static final String HAZELCAST_4_TOPIC = "com.hazelcast.topic.ITopic";

        private static final String HAZELCAST_3_MESSAGE_LISTENER = "com.hazelcast.core.MessageListener";
        private static final String HAZELCAST_3_MESSAGE = "com.hazelcast.core.Message";
        private static final String HAZELCAST_3_TOPIC = "com.hazelcast.core.ITopic";

        private static final MethodHandles.Lookup LOOKUP = MethodHandles.publicLookup();

        private final Class<?> messageListenerClass;
        private final MethodHandle getMessageObjectHandle;
        private final MethodHandle getTopicHandle;
        private final MethodHandle publishHandle;
        private final MethodHandle addMessageListenerHandle;
        private final MethodHandle removeMessageListenerHandle;
        private final boolean usesUuidForRegistration;

        private static final class Holder {
            static final HazelcastMethodHandles INSTANCE = new HazelcastMethodHandles();
        }

        private HazelcastMethodHandles() {
            HazelcastVersion version = detectVersion();

            this.messageListenerClass = version.messageListenerClass;
            this.getMessageObjectHandle = version.getMessageObjectHandle;
            this.getTopicHandle = findGetTopicHandle(version.topicClass);
            this.publishHandle = findPublishHandle(version.topicClass);
            this.addMessageListenerHandle = findAddListenerHandle(version.topicClass, messageListenerClass);

            MethodHandleWithType removeResult = findRemoveListenerHandle(version.topicClass);
            this.removeMessageListenerHandle = removeResult.handle;
            this.usesUuidForRegistration = removeResult.usesUuid;
        }

        private static HazelcastMethodHandles getInstance() {
            return Holder.INSTANCE;
        }

        public static Object getTopic(HazelcastInstance hazelcast, String name) {
            try {
                return getInstance().getTopicHandle.invoke(hazelcast, name);
            } catch (Throwable t) {
                throw propagate(t, "Failed to get topic: " + name);
            }
        }

        public static void publish(Object topic, Object message) {
            try {
                getInstance().publishHandle.invoke(topic, message);
            } catch (Throwable t) {
                throw propagate(t, "Failed to publish message");
            }
        }

        public static <T> Object createMessageListener(Consumer<T> handler) {
            HazelcastMethodHandles instance = getInstance();
            return Proxy.newProxyInstance(
                    instance.messageListenerClass.getClassLoader(),
                    new Class<?>[]{instance.messageListenerClass},
                    new MessageListenerInvocationHandler<>(handler, instance.getMessageObjectHandle)
            );
        }

        public static String addMessageListener(Object topic, Object listener) {
            try {
                Object registrationId = getInstance().addMessageListenerHandle.invoke(topic, listener);
                return registrationId.toString();
            } catch (Throwable t) {
                throw propagate(t, "Failed to add message listener");
            }
        }

        public static boolean removeMessageListener(Object topic, String registrationId) {
            HazelcastMethodHandles instance = getInstance();
            try {
                Object param = instance.usesUuidForRegistration
                        ? UUID.fromString(registrationId)
                        : registrationId;

                Object result = instance.removeMessageListenerHandle.invoke(topic, param);
                return result instanceof Boolean ? (Boolean) result : true;
            } catch (Throwable t) {
                throw propagate(t, "Failed to remove message listener: " + registrationId);
            }
        }

        private static HazelcastVersion detectVersion() {
            // Try Hazelcast 4.x/5.x first
            HazelcastVersion version = tryLoadVersion(HAZELCAST_4_MESSAGE_LISTENER, HAZELCAST_4_MESSAGE, HAZELCAST_4_TOPIC);
            if (version != null) {
                return version;
            }

            // Fall back to Hazelcast 3.x
            version = tryLoadVersion(HAZELCAST_3_MESSAGE_LISTENER, HAZELCAST_3_MESSAGE, HAZELCAST_3_TOPIC);
            if (version != null) {
                return version;
            }

            throw new IllegalStateException(
                    "Hazelcast not found on classpath. Ensure Hazelcast 3.x, 4.x, or 5.x is available.");
        }

        private static HazelcastVersion tryLoadVersion(String listenerClassName, String messageClassName, String topicClassName) {
            try {
                Class<?> listenerClass = Class.forName(listenerClassName);
                Class<?> messageClass = Class.forName(messageClassName);
                Class<?> topicClass = Class.forName(topicClassName);

                MethodHandle getMessageObject = LOOKUP.findVirtual(
                        messageClass,
                        "getMessageObject",
                        MethodType.methodType(Object.class)
                );

                return new HazelcastVersion(listenerClass, topicClass, getMessageObject);
            } catch (ClassNotFoundException | NoSuchMethodException | IllegalAccessException e) {
                return null;
            }
        }

        private static MethodHandle findGetTopicHandle(Class<?> topicClass) {
            try {
                return LOOKUP.findVirtual(
                        HazelcastInstance.class,
                        "getTopic",
                        MethodType.methodType(topicClass, String.class)
                );
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalStateException("Method not found: HazelcastInstance.getTopic", e);
            }
        }

        private static MethodHandle findPublishHandle(Class<?> topicClass) {
            try {
                return LOOKUP.findVirtual(
                        topicClass,
                        "publish",
                        MethodType.methodType(void.class, Object.class)
                );
            } catch (NoSuchMethodException | IllegalAccessException e) {
                throw new IllegalStateException("Method not found: ITopic.publish", e);
            }
        }

        private static MethodHandle findAddListenerHandle(Class<?> topicClass, Class<?> listenerClass) {
            try {
                // Try UUID return type first (Hazelcast 4.x/5.x)
                return LOOKUP.findVirtual(
                        topicClass,
                        "addMessageListener",
                        MethodType.methodType(UUID.class, listenerClass)
                );
            } catch (NoSuchMethodException | IllegalAccessException e) {
                // Fall back to String return type (Hazelcast 3.x)
                try {
                    return LOOKUP.findVirtual(
                            topicClass,
                            "addMessageListener",
                            MethodType.methodType(String.class, listenerClass)
                    );
                } catch (NoSuchMethodException | IllegalAccessException ex) {
                    throw new IllegalStateException("Method not found: ITopic.addMessageListener", ex);
                }
            }
        }

        private static MethodHandleWithType findRemoveListenerHandle(Class<?> topicClass) {
            // Try UUID parameter first (Hazelcast 4.x/5.x)
            try {
                MethodHandle handle = LOOKUP.findVirtual(
                        topicClass,
                        "removeMessageListener",
                        MethodType.methodType(boolean.class, UUID.class)
                );
                return new MethodHandleWithType(handle, true);
            } catch (NoSuchMethodException | IllegalAccessException e) {
                // Fall back to String parameter (Hazelcast 3.x)
                try {
                    MethodHandle handle = LOOKUP.findVirtual(
                            topicClass,
                            "removeMessageListener",
                            MethodType.methodType(boolean.class, String.class)
                    );
                    return new MethodHandleWithType(handle, false);
                } catch (NoSuchMethodException | IllegalAccessException ex) {
                    throw new IllegalStateException("Method not found: ITopic.removeMessageListener", ex);
                }
            }
        }

        private static RuntimeException propagate(Throwable t, String message) {
            if (t instanceof RuntimeException) {
                return (RuntimeException) t;
            }
            if (t instanceof Error) {
                throw (Error) t;
            }
            return new IllegalStateException(message, t);
        }

        private static final class HazelcastVersion {
            final Class<?> messageListenerClass;
            final Class<?> topicClass;
            final MethodHandle getMessageObjectHandle;

            HazelcastVersion(Class<?> messageListenerClass, Class<?> topicClass, MethodHandle getMessageObjectHandle) {
                this.messageListenerClass = messageListenerClass;
                this.topicClass = topicClass;
                this.getMessageObjectHandle = getMessageObjectHandle;
            }
        }

        private static final class MethodHandleWithType {
            final MethodHandle handle;
            final boolean usesUuid;

            MethodHandleWithType(MethodHandle handle, boolean usesUuid) {
                this.handle = handle;
                this.usesUuid = usesUuid;
            }
        }

        private static final class MessageListenerInvocationHandler<T> implements InvocationHandler {

            private final Consumer<T> handler;
            private final MethodHandle getMessageObjectHandle;

            MessageListenerInvocationHandler(Consumer<T> handler, MethodHandle getMessageObjectHandle) {
                this.handler = handler;
                this.getMessageObjectHandle = getMessageObjectHandle;
            }

            @Override
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                String methodName = method.getName();

                if ("onMessage".equals(methodName)) {
                    if (args != null && args.length == 1) {
                        handleMessage(args[0]);
                    }
                    return null;
                }

                if ("equals".equals(methodName)) {
                    return args != null && args.length == 1 && proxy == args[0];
                }

                if ("hashCode".equals(methodName)) {
                    return System.identityHashCode(proxy);
                }

                if ("toString".equals(methodName)) {
                    return "HazelcastMessageListenerProxy@" + Integer.toHexString(System.identityHashCode(proxy));
                }

                return null;
            }

            @SuppressWarnings("unchecked")
            private void handleMessage(Object message) throws Throwable {
                T messageObject = (T) getMessageObjectHandle.invoke(message);
                handler.accept(messageObject);
            }
        }
    }
}