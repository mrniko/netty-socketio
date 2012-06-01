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
package com.corundumstudio.socketio.listener;

import java.util.Queue;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ConcurrentMap;

import com.corundumstudio.socketio.SocketIOClient;

public class ListenersHub implements ClientListeners {

    private final ConcurrentMap<String, Queue<DataListener<Object>>> eventListeners =
                                                        new ConcurrentHashMap<String, Queue<DataListener<Object>>>();
    private final Queue<DataListener<Object>> jsonObjectListeners = new ConcurrentLinkedQueue<DataListener<Object>>();
    private final Queue<DataListener<String>> messageListeners = new ConcurrentLinkedQueue<DataListener<String>>();
    private final Queue<ConnectListener> connectListeners = new ConcurrentLinkedQueue<ConnectListener>();
    private final Queue<DisconnectListener> disconnectListeners = new ConcurrentLinkedQueue<DisconnectListener>();

    @Override
    public void addEventListener(String eventName, DataListener<Object> listener) {
        Queue<DataListener<Object>> entry = eventListeners.get(eventName);
        if (entry == null) {
            entry = new ConcurrentLinkedQueue<DataListener<Object>>();
            Queue<DataListener<Object>> oldEntry = eventListeners.putIfAbsent(eventName, entry);
            if (oldEntry != null) {
                entry = oldEntry;
            }
        }
        entry.add(listener);
    }

    public void onEvent(SocketIOClient client, String eventName, Object data) {
        Queue<DataListener<Object>> entry = eventListeners.get(eventName);
        for (DataListener<Object> dataListener : entry) {
            dataListener.onData(client, data);
        }
    }

    @Override
    public void addJsonObjectListener(DataListener<Object> listener) {
        jsonObjectListeners.add(listener);
    }

    public Queue<DataListener<Object>> getJsonObjectListeners() {
        return jsonObjectListeners;
    }

    @Override
    public void addDisconnectListener(DisconnectListener listener) {
        disconnectListeners.add(listener);
    }

    public void onDisconnect(SocketIOClient client) {
        for (DisconnectListener listener : disconnectListeners) {
            listener.onDisconnect(client);
        }
    }

    @Override
    public void addConnectListener(ConnectListener listener) {
        connectListeners.add(listener);
    }

    public void onConnect(SocketIOClient client) {
        for (ConnectListener listener : connectListeners) {
            listener.onConnect(client);
        }
    }

    @Override
    public void addMessageListener(DataListener<String> listener) {
        messageListeners.add(listener);
    }

    public Queue<DataListener<String>> getMessageListeners() {
        return messageListeners;
    }

    public void onMessage(SocketIOClient client, String data) {
        for (DataListener<String> listener : messageListeners) {
            listener.onData(client, data);
        }
    }

    public void onJsonObject(SocketIOClient client, Object data) {
        for (DataListener<Object> listener : jsonObjectListeners) {
            listener.onData(client, data);
        }
    }

}
