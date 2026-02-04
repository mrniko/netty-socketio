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

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
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
 * <p>
 * Uses reflection to handle API differences between versions:
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
        Object topic = HazelcastReflection.getTopic(hazelcastPub, type.toString());
        HazelcastReflection.publish(topic, message);
    }

    @Override
    public <T extends PubSubMessage> void subscribe(PubSubType type, PubSubListener<T> listener, Class<T> clazz) {
        String topicName = type.toString();
        Object topic = HazelcastReflection.getTopic(hazelcastSub, topicName);

        Object hazelcastListener = HazelcastReflection.createMessageListener(
                new FilteredMessageHandler<>(nodeId, listener)
        );

        String registrationId = HazelcastReflection.addMessageListener(topic, hazelcastListener);

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

        Object topic = HazelcastReflection.getTopic(hazelcastSub, topicName);
        for (String registrationId : registrationIds) {
            HazelcastReflection.removeMessageListener(topic, registrationId);
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

    static final class HazelcastReflection {

        private static final String HAZELCAST_4_MESSAGE_LISTENER = "com.hazelcast.topic.MessageListener";
        private static final String HAZELCAST_4_MESSAGE = "com.hazelcast.topic.Message";
        private static final String HAZELCAST_4_TOPIC = "com.hazelcast.topic.ITopic";

        private static final String HAZELCAST_3_MESSAGE_LISTENER = "com.hazelcast.core.MessageListener";
        private static final String HAZELCAST_3_MESSAGE = "com.hazelcast.core.Message";
        private static final String HAZELCAST_3_TOPIC = "com.hazelcast.core.ITopic";

        private static final String METHOD_GET_TOPIC = "getTopic";
        private static final String METHOD_PUBLISH = "publish";
        private static final String METHOD_ADD_LISTENER = "addMessageListener";
        private static final String METHOD_REMOVE_LISTENER = "removeMessageListener";
        private static final String METHOD_GET_MESSAGE_OBJECT = "getMessageObject";

        private final Class<?> messageListenerClass;
        private final Method getMessageObjectMethod;
        private final Method getTopicMethod;
        private final Method publishMethod;
        private final Method addMessageListenerMethod;
        private final Method removeMessageListenerMethod;
        private final boolean usesUuidForRegistration;

        private static final class Holder {
            static final HazelcastReflection INSTANCE = new HazelcastReflection();
        }

        private HazelcastReflection() {
            HazelcastVersion version = detectVersion();

            this.messageListenerClass = version.messageListenerClass;
            this.getMessageObjectMethod = version.getMessageObjectMethod;
            this.getTopicMethod = resolveGetTopicMethod();
            this.publishMethod = resolveMethod(version.topicClass, METHOD_PUBLISH, Object.class);
            this.addMessageListenerMethod = resolveMethod(version.topicClass, METHOD_ADD_LISTENER, messageListenerClass);
            this.removeMessageListenerMethod = resolveRemoveListenerMethod(version.topicClass);
            this.usesUuidForRegistration = removeMessageListenerMethod.getParameterTypes()[0] == UUID.class;
        }

        private static HazelcastReflection getInstance() {
            return Holder.INSTANCE;
        }

        public static Object getTopic(HazelcastInstance hazelcast, String name) {
            return invoke(getInstance().getTopicMethod, hazelcast, name);
        }

        public static void publish(Object topic, Object message) {
            invoke(getInstance().publishMethod, topic, message);
        }

        public static <T> Object createMessageListener(Consumer<T> handler) {
            HazelcastReflection instance = getInstance();
            return Proxy.newProxyInstance(
                    instance.messageListenerClass.getClassLoader(),
                    new Class<?>[]{instance.messageListenerClass},
                    new MessageListenerInvocationHandler<>(handler, instance.getMessageObjectMethod)
            );
        }

        public static String addMessageListener(Object topic, Object listener) {
            Object registrationId = invoke(getInstance().addMessageListenerMethod, topic, listener);
            return registrationId.toString();
        }

        public static boolean removeMessageListener(Object topic, String registrationId) {
            HazelcastReflection instance = getInstance();
            Object param = instance.usesUuidForRegistration
                    ? UUID.fromString(registrationId)
                    : registrationId;

            Object result = invoke(instance.removeMessageListenerMethod, topic, param);
            return result instanceof Boolean ? (Boolean) result : true;
        }

        private static HazelcastVersion detectVersion() {
            // Try Hazelcast 4.x/5.x first
            try {
                return loadVersion(HAZELCAST_4_MESSAGE_LISTENER, HAZELCAST_4_MESSAGE, HAZELCAST_4_TOPIC);
            } catch (ReflectiveOperationException e) {
                // Fall back to Hazelcast 3.x
                try {
                    return loadVersion(HAZELCAST_3_MESSAGE_LISTENER, HAZELCAST_3_MESSAGE, HAZELCAST_3_TOPIC);
                } catch (ReflectiveOperationException ex) {
                    throw new IllegalStateException(
                            "Hazelcast not found on classpath. Ensure Hazelcast 3.x, 4.x, or 5.x is available.", ex);
                }
            }
        }

        private static HazelcastVersion loadVersion(String listenerClassName, String messageClassName, String topicClassName) throws ReflectiveOperationException {
            Class<?> listenerClass = Class.forName(listenerClassName);
            Class<?> messageClass = Class.forName(messageClassName);
            Class<?> topicClass = Class.forName(topicClassName);
            Method getMessageObject = messageClass.getMethod(METHOD_GET_MESSAGE_OBJECT);

            return new HazelcastVersion(listenerClass, messageClass, topicClass, getMessageObject);
        }

        private static Method resolveGetTopicMethod() {
            try {
                return HazelcastInstance.class.getMethod(METHOD_GET_TOPIC, String.class);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Method not found: HazelcastInstance." + METHOD_GET_TOPIC, e);
            }
        }

        private static Method resolveMethod(Class<?> clazz, String methodName, Class<?>... paramTypes) {
            try {
                return clazz.getMethod(methodName, paramTypes);
            } catch (NoSuchMethodException e) {
                throw new IllegalStateException("Method not found: " + clazz.getSimpleName() + "." + methodName, e);
            }
        }

        private static Method resolveRemoveListenerMethod(Class<?> topicClass) {
            // Try UUID parameter first (Hazelcast 4.x/5.x)
            try {
                return topicClass.getMethod(METHOD_REMOVE_LISTENER, UUID.class);
            } catch (NoSuchMethodException e) {
                // Fall back to String parameter (Hazelcast 3.x)
                return resolveMethod(topicClass, METHOD_REMOVE_LISTENER, String.class);
            }
        }

        private static Object invoke(Method method, Object target, Object... args) {
            try {
                return method.invoke(target, args);
            } catch (IllegalAccessException e) {
                throw new IllegalStateException("Cannot access method: " + method.getName(), e);
            } catch (InvocationTargetException e) {
                throw unwrapException(e);
            }
        }

        private static RuntimeException unwrapException(InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException) {
                return (RuntimeException) cause;
            }
            if (cause instanceof Error) {
                throw (Error) cause;
            }
            return new IllegalStateException("Invocation failed", cause != null ? cause : e);
        }

        private static final class HazelcastVersion {
            final Class<?> messageListenerClass;
            final Class<?> messageClass;
            final Class<?> topicClass;
            final Method getMessageObjectMethod;

            HazelcastVersion(Class<?> messageListenerClass, Class<?> messageClass,
                             Class<?> topicClass, Method getMessageObjectMethod) {
                this.messageListenerClass = messageListenerClass;
                this.messageClass = messageClass;
                this.topicClass = topicClass;
                this.getMessageObjectMethod = getMessageObjectMethod;
            }
        }

        private static final class MessageListenerInvocationHandler<T> implements InvocationHandler {

            private final Consumer<T> handler;
            private final Method getMessageObjectMethod;

            MessageListenerInvocationHandler(Consumer<T> handler, Method getMessageObjectMethod) {
                this.handler = handler;
                this.getMessageObjectMethod = getMessageObjectMethod;
            }

            @Override
            @SuppressWarnings("unchecked")
            public Object invoke(Object proxy, Method method, Object[] args) throws Throwable {
                switch (method.getName()) {
                    case "onMessage":
                        if (args != null && args.length == 1) {
                            handleMessage(args[0]);
                        }
                        return null;

                    case "equals":
                        return proxy == args[0];

                    case "hashCode":
                        return System.identityHashCode(proxy);

                    case "toString":
                        return "HazelcastMessageListenerProxy@" + Integer.toHexString(System.identityHashCode(proxy));

                    default:
                        return null;
                }
            }

            @SuppressWarnings("unchecked")
            private void handleMessage(Object message) throws Throwable {
                try {
                    T messageObject = (T) getMessageObjectMethod.invoke(message);
                    handler.accept(messageObject);
                } catch (InvocationTargetException e) {
                    Throwable cause = e.getCause();
                    throw cause != null ? cause : e;
                }
            }
        }
    }

}