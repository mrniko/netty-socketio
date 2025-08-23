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
package com.corundumstudio.socketio.namespace;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.corundumstudio.socketio.listener.DataListener;

public class EventEntry<T> {

    private final Queue<DataListener<T>> listeners = new ConcurrentLinkedQueue<DataListener<T>>();;

    public EventEntry() {
        super();
    }

    public void addListener(DataListener<T> listener) {
        listeners.add(listener);
    }

    public Queue<DataListener<T>> getListeners() {
        return listeners;
    }

}
