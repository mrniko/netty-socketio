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
import java.util.Collection;
import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIONamespace;
import com.corundumstudio.socketio.misc.CompositeIterable;

public class NamespacesHub {

    private final ConcurrentMap<String, SocketIONamespace> namespaces = new ConcurrentHashMap<String, SocketIONamespace>();
    private final Configuration configuration;

    public NamespacesHub(Configuration configuration) {
        this.configuration = configuration;
    }

    public Namespace create(String name) {
        Namespace namespace = (Namespace) namespaces.get(name);
        if (namespace == null) {
            namespace = new Namespace(name, configuration);
            Namespace oldNamespace = (Namespace) namespaces.putIfAbsent(name, namespace);
            if (oldNamespace != null) {
                namespace = oldNamespace;
            }
        }
        return namespace;
    }

    public Iterable<SocketIOClient> getRoomClients(String room) {
        List<Iterable<SocketIOClient>> allClients = new ArrayList<Iterable<SocketIOClient>>();
        for (SocketIONamespace namespace : namespaces.values()) {
            Iterable<SocketIOClient> clients = ((Namespace)namespace).getRoomClients(room);
            allClients.add(clients);
        }
        return new CompositeIterable<SocketIOClient>(allClients);
    }

    public Namespace get(String name) {
        return (Namespace) namespaces.get(name);
    }

    public void remove(String name) {
        SocketIONamespace namespace = namespaces.remove(name);
        if (namespace != null) {
            namespace.getBroadcastOperations().disconnect();
        }
    }

    public Collection<SocketIONamespace> getAllNamespaces() {
        return namespaces.values();
    }

}
