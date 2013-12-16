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
package com.corundumstudio.socketio.namespace;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Queue;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.annotation.ScannerEngine;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.corundumstudio.socketio.parser.JsonSupport;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.store.StoreFactory;
import com.corundumstudio.socketio.store.pubsub.DispatchMessage;
import com.corundumstudio.socketio.store.pubsub.JoinLeaveMessage;
import com.corundumstudio.socketio.store.pubsub.PubSubStore;
import com.corundumstudio.socketio.transport.NamespaceClient;

/**
 * Hub object for all clients in one namespace.
 * Namespace shares by different namespace-clients.
 *
 * @see com.corundumstudio.socketio.transport.NamespaceClient
 */
public class Namespace implements SocketIONamespace {

    public static final String DEFAULT_NAME = "";

    private final Map<UUID, SocketIOClient> allClients = new ConcurrentHashMap<UUID, SocketIOClient>();
    private final ConcurrentMap<String, EventEntry<?>> eventListeners =
                                                            new ConcurrentHashMap<String, EventEntry<?>>();
    private final ConcurrentMap<Class<?>, Queue<DataListener<?>>> jsonObjectListeners =
                                                            new ConcurrentHashMap<Class<?>, Queue<DataListener<?>>>();
    private final Queue<DataListener<String>> messageListeners = new ConcurrentLinkedQueue<DataListener<String>>();
    private final Queue<ConnectListener> connectListeners = new ConcurrentLinkedQueue<ConnectListener>();
    private final Queue<DisconnectListener> disconnectListeners = new ConcurrentLinkedQueue<DisconnectListener>();

    // TODO user Set<UUID>
    private final ConcurrentMap<String, Queue<UUID>> roomClients = new ConcurrentHashMap<String, Queue<UUID>>();

    private final String name;
    private final JsonSupport jsonSupport;
    private final StoreFactory storeFactory;

    public Namespace(String name, JsonSupport jsonSupport, StoreFactory storeFactory) {
        super();
        this.name = name;
        this.jsonSupport = jsonSupport;
        this.storeFactory = storeFactory;
    }

    public void addClient(SocketIOClient client) {
        allClients.put(client.getSessionId(), client);
    }

    public String getName() {
        return name;
    }

    @Override
    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> void addEventListener(String eventName, Class<T> eventClass, DataListener<T> listener) {
        EventEntry entry = eventListeners.get(eventName);
        if (entry == null) {
            entry = new EventEntry<T>(eventClass);
            EventEntry<?> oldEntry = eventListeners.putIfAbsent(eventName, entry);
            if (oldEntry != null) {
                entry = oldEntry;
            }
        }
        entry.addListener(listener);
        jsonSupport.addEventMapping(eventName, eventClass);
    }

    @Override
    public <T> void addJsonObjectListener(Class<T> clazz, DataListener<T> listener) {
        Queue<DataListener<?>> queue = jsonObjectListeners.get(clazz);
        if (queue == null) {
            queue = new ConcurrentLinkedQueue<DataListener<?>>();
            Queue<DataListener<?>> oldQueue = jsonObjectListeners.putIfAbsent(clazz, queue);
            if (oldQueue != null) {
                queue = oldQueue;
            }
        }
        queue.add(listener);
        jsonSupport.addJsonClass(clazz);
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onEvent(NamespaceClient client, String eventName, Object data, AckRequest ackRequest) {
        EventEntry entry = eventListeners.get(eventName);
        if (entry == null) {
            return;
        }
        Queue<DataListener> listeners = entry.getListeners();
        for (DataListener dataListener : listeners) {
            dataListener.onData(client, data, ackRequest);
        }
    }

    public void onMessage(NamespaceClient client, String data, AckRequest ackRequest) {
        for (DataListener<String> listener : messageListeners) {
            listener.onData(client, data, ackRequest);
        }
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onJsonObject(NamespaceClient client, Object data, AckRequest ackRequest) {
        Queue<DataListener<?>> queue = jsonObjectListeners.get(data.getClass());
        if (queue == null) {
            return;
        }
        for (DataListener dataListener : queue) {
            dataListener.onData(client, data, ackRequest);
        }
    }

    @Override
    public void addDisconnectListener(DisconnectListener listener) {
        disconnectListeners.add(listener);
    }

    public void onDisconnect(SocketIOClient client) {
        for (DisconnectListener listener : disconnectListeners) {
            listener.onDisconnect(client);
        }
        allClients.remove(client);

        leave(getName(), client.getSessionId());
        storeFactory.getPubSubStore().publish(PubSubStore.LEAVE, new JoinLeaveMessage(client.getSessionId(), getName()));
    }

    @Override
    public void addConnectListener(ConnectListener listener) {
        connectListeners.add(listener);
    }

    public void onConnect(SocketIOClient client) {
        for (ConnectListener listener : connectListeners) {
            listener.onConnect(client);
        }

        join(getName(), client.getSessionId());
        storeFactory.getPubSubStore().publish(PubSubStore.JOIN, new JoinLeaveMessage(client.getSessionId(), getName()));
    }

    @Override
    public void addMessageListener(DataListener<String> listener) {
        messageListeners.add(listener);
    }

    public Queue<DataListener<String>> getMessageListeners() {
        return messageListeners;
    }

    @Override
    public BroadcastOperations getBroadcastOperations() {
        return new BroadcastOperations(allClients.values(), storeFactory);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj)
            return true;
        if (obj == null)
            return false;
        if (getClass() != obj.getClass())
            return false;
        Namespace other = (Namespace) obj;
        if (name == null) {
            if (other.name != null)
                return false;
        } else if (!name.equals(other.name))
            return false;
        return true;
    }

    @Override
    public void addListeners(Object listeners) {
        ScannerEngine engine = new ScannerEngine();
        engine.scan(this, listeners, listeners.getClass());
    }

    @Override
    public void addListeners(Object listeners, Class listenersClass) {
        ScannerEngine engine = new ScannerEngine();
        engine.scan(this, listeners, listenersClass);
    }

    public void joinRoom(String room, UUID sessionId) {
        room += getName() + "/" + room;

        join(room, sessionId);
        storeFactory.getPubSubStore().publish(PubSubStore.JOIN, new JoinLeaveMessage(sessionId, room));
    }

    public void doDispatch(String room, Packet packet) {
        if (room != null && !room.isEmpty()) {
            room += getName() + "/" + room;
        }
        storeFactory.getPubSubStore().publish(PubSubStore.DISPATCH, new DispatchMessage(room, packet));
    }

    public void dispatch(String room, Packet packet) {
        Iterable<SocketIOClient> clients = getRoomClients(room);

        for (SocketIOClient socketIOClient : clients) {
            socketIOClient.send(packet);
        }
    }

    public void join(String room, UUID sessionId) {
        Queue<UUID> clients = roomClients.get(room);
        if (clients == null) {
            clients = new ConcurrentLinkedQueue<UUID>();
            Queue<UUID> oldClients = roomClients.putIfAbsent(room, clients);
            if (oldClients != null) {
                clients = oldClients;
            }
        }
        clients.add(sessionId);
        // object may be changed due to other concurrent call
        if (clients != roomClients.get(room)) {
            // re-join if queue has been replaced
            joinRoom(room, sessionId);
        }
    }

    public void leaveRoom(String room, UUID sessionId) {
        room += getName() + "/" + room;

        leave(room, sessionId);
        storeFactory.getPubSubStore().publish(PubSubStore.LEAVE, new JoinLeaveMessage(sessionId, room));
    }

    public void leave(String room, UUID sessionId) {
        Queue<UUID> clients = roomClients.get(room);
        if (clients == null) {
            return;
        }
        clients.remove(sessionId);
        if (clients.isEmpty()) {
            clients = roomClients.remove(room);
            // join which was added after queue deletion
            for (UUID clientId : clients) {
                joinRoom(room, clientId);
            }
        }
    }

    // TODO optimize
    public List<String> getRooms(SocketIOClient client) {
        List<String> result = new ArrayList<String>();
        for (Entry<String, Queue<UUID>> entry : roomClients.entrySet()) {
            if (entry.getValue().contains(client.getSessionId())) {
                result.add(entry.getKey());
            }
        }
        return result;
    }

    public Iterable<SocketIOClient> getRoomClients(String room) {
        Queue<UUID> sessionIds = roomClients.get(room);

        if (sessionIds == null) {
            return Collections.emptyList();
        }

        List<SocketIOClient> result = new ArrayList<SocketIOClient>();
        for (SocketIOClient client : allClients.values()) {
            if (sessionIds.contains(client.getSessionId())) {
                result.add(client);
            }
        }
        return result;
    }

    // Utility function to check if there are anymore clients in namespace
    public boolean isEmpty(){
        return allClients.isEmpty();
    }

}
