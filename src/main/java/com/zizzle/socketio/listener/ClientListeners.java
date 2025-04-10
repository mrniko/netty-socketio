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
package com.zizzle.socketio.listener;


public interface ClientListeners {

    void addMultiTypeEventListener(String eventName, MultiTypeEventListener listener, Class<?> ... eventClass);

    <T> void addEventListener(String eventName, Class<T> eventClass, DataListener<T> listener);

    void addEventInterceptor(EventInterceptor eventInterceptor);

    void addDisconnectListener(DisconnectListener listener);

    void addConnectListener(ConnectListener listener);

    /**
     * from v4, ping will always be sent by server except probe ping packet sent from client,
     * and pong will always be responded by client while receiving ping except probe pong packet responded from server
     * it makes no more sense to listen to ping packet, instead you can listen to pong packet
     * @deprecated use addPongListener instead
     * @param listener
     */
    @Deprecated
    void addPingListener(PingListener listener);
    void addPongListener(PongListener listener);

    void addListeners(Object listeners);

    <L> void addListeners(Iterable<L> listeners);

    void addListeners(Object listeners, Class<?> listenersClass);

    void removeAllListeners(String eventName);
    
}
