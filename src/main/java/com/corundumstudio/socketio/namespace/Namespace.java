/**
 * Copyright (c) 2012-2019 Nikita Koksharov
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
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import com.corundumstudio.socketio.AckMode;
import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.BroadcastOperations;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.MultiTypeArgs;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.annotation.ScannerEngine;
import com.corundumstudio.socketio.listener.*;
import com.corundumstudio.socketio.protocol.JsonSupport;
import com.corundumstudio.socketio.protocol.Packet;
import com.corundumstudio.socketio.store.StoreFactory;
import com.corundumstudio.socketio.store.pubsub.JoinLeaveMessage;
import com.corundumstudio.socketio.store.pubsub.PubSubType;
import com.corundumstudio.socketio.transport.NamespaceClient;

import io.netty.util.internal.PlatformDependent;

/**
 * Hub object for all clients in one namespace.
 * Namespace shares by different namespace-clients.
 *
 * @see com.corundumstudio.socketio.transport.NamespaceClient
 */
public class Namespace implements SocketIONamespace {

    public static final String DEFAULT_NAME = "";
       
    private ListenerManager listenerManager;
    
    private final Map<UUID, SocketIOClient> allClients = PlatformDependent.newConcurrentHashMap();
    private final ConcurrentMap<String, Set<UUID>> roomClients = PlatformDependent.newConcurrentHashMap();
    private final ConcurrentMap<UUID, Set<String>> clientRooms = PlatformDependent.newConcurrentHashMap();

    private final String name;
    private final StoreFactory storeFactory;

    public Namespace(String name, Configuration configuration) {
        super();

        this.name = name;
        this.storeFactory = configuration.getStoreFactory();
    
        listenerManager = new ListenerManager(this, configuration.getJsonSupport(), name,
        		configuration.getExceptionListener(), configuration.getAckMode());
    }

    public void addClient(SocketIOClient client) {
        allClients.put(client.getSessionId(), client);
    }

    @Override
    public String getName() {
        return name;
    }
//
//    @Override
//    public void addMultiTypeEventListener(String eventName, MultiTypeEventListener listener,
//            Class<?>... eventClass) {
//    	listenerManager.addMultiTypeEventListener(eventName, listener, eventClass);
//    }
//    
//    @Override
//    public void removeAllListeners(String eventName) {
//    	listenerManager.removeAllListeners(eventName);
//    }
//
//    @Override
//    @SuppressWarnings({"unchecked", "rawtypes"})
//    public <T> void addEventListener(String eventName, Class<T> eventClass, DataListener<T> listener) {
//    	listenerManager.addEventListener(eventName, eventClass, listener);
//    }
//
//    @Override
//    public void addEventInterceptor(EventInterceptor eventInterceptor) {
//    	listenerManager.addEventInterceptor(eventInterceptor);
//    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    public void onEvent(NamespaceClient client, String eventName, List<Object> args, AckRequest ackRequest) {
        if(listenerManager.onEvent(client, eventName, args, ackRequest))
        	listenerManager.sendAck(ackRequest);
        return;
    }


    public void onDisconnect(SocketIOClient client) {
        Set<String> joinedRooms = client.getAllRooms();        
        allClients.remove(client.getSessionId());

        leave(getName(), client.getSessionId());
        storeFactory.pubSubStore().publish(PubSubType.LEAVE, new JoinLeaveMessage(client.getSessionId(), getName(), getName()));

        for (String joinedRoom : joinedRooms) {
            leave(roomClients, joinedRoom, client.getSessionId());
        }
        clientRooms.remove(client.getSessionId());

        listenerManager.onDisconnect(client);
    }



    public void onConnect(SocketIOClient client) {
        join(getName(), client.getSessionId());
        storeFactory.pubSubStore().publish(PubSubType.JOIN, new JoinLeaveMessage(client.getSessionId(), getName(), getName()));

        listenerManager.onConnect(client);

    }

//    @Override
//    public void addPingListener(PingListener listener) {
//        pingListeners.add(listener);
//    }

    public void onPing(SocketIOClient client) {
        listenerManager.onPing(client);
    }

    @Override
    public BroadcastOperations getBroadcastOperations() {
        return new BroadcastOperations(allClients.values(), storeFactory);
    }

    @Override
    public BroadcastOperations getRoomOperations(String room) {
        return new BroadcastOperations(getRoomClients(room), storeFactory);
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

//    @Override
//    public void addListeners(Object listeners) {
//        addListeners(listeners, listeners.getClass());
//    }
//
//    @Override
//    public void addListeners(Object listeners, Class<?> listenersClass) {
//        engine.scan(this, listeners, listenersClass);
//    }

    public void joinRoom(String room, UUID sessionId) {
        join(room, sessionId);
        storeFactory.pubSubStore().publish(PubSubType.JOIN, new JoinLeaveMessage(sessionId, room, getName()));
    }

    public void dispatch(String room, Packet packet) {
        Iterable<SocketIOClient> clients = getRoomClients(room);

        for (SocketIOClient socketIOClient : clients) {
            socketIOClient.send(packet);
        }
    }

    private <K, V> void join(ConcurrentMap<K, Set<V>> map, K key, V value) {
        Set<V> clients = map.get(key);
        if (clients == null) {
            clients = Collections.newSetFromMap(PlatformDependent.<V, Boolean>newConcurrentHashMap());
            Set<V> oldClients = map.putIfAbsent(key, clients);
            if (oldClients != null) {
                clients = oldClients;
            }
        }
        clients.add(value);
        // object may be changed due to other concurrent call
        if (clients != map.get(key)) {
            // re-join if queue has been replaced
            join(map, key, value);
        }
    }

    public void join(String room, UUID sessionId) {
        join(roomClients, room, sessionId);
        join(clientRooms, sessionId, room);
    }

    public void leaveRoom(String room, UUID sessionId) {
        leave(room, sessionId);
        storeFactory.pubSubStore().publish(PubSubType.LEAVE, new JoinLeaveMessage(sessionId, room, getName()));
    }

    private <K, V> void leave(ConcurrentMap<K, Set<V>> map, K room, V sessionId) {
        Set<V> clients = map.get(room);
        if (clients == null) {
            return;
        }
        clients.remove(sessionId);

        if (clients.isEmpty()) {
            map.remove(room, Collections.emptySet());
        }
    }

    public void leave(String room, UUID sessionId) {
        leave(roomClients, room, sessionId);
        leave(clientRooms, sessionId, room);
    }

    public Set<String> getRooms(SocketIOClient client) {
        Set<String> res = clientRooms.get(client.getSessionId());
        if (res == null) {
            return Collections.emptySet();
        }
        return Collections.unmodifiableSet(res);
    }

    public Set<String> getRooms() {
        return roomClients.keySet();
    }

    public Iterable<SocketIOClient> getRoomClients(String room) {
        Set<UUID> sessionIds = roomClients.get(room);

        if (sessionIds == null) {
            return Collections.emptyList();
        }

        List<SocketIOClient> result = new ArrayList<SocketIOClient>();
        for (UUID sessionId : sessionIds) {
            SocketIOClient client = allClients.get(sessionId);
            if(client != null) {
                result.add(client);
            }
        }
        return result;
    }

    @Override
    public Collection<SocketIOClient> getAllClients() {
        return Collections.unmodifiableCollection(allClients.values());
    }

    @Override
    public SocketIOClient getClient(UUID uuid) {
        return allClients.get(uuid);
    }

	@Override
	public ListenerManager getListenerManager() {
		// TODO Auto-generated method stub
		return listenerManager;
	}

}
