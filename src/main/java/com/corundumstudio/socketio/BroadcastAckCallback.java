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
package com.corundumstudio.socketio;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

public class BroadcastAckCallback<T> {

    final AtomicBoolean loopFinished = new AtomicBoolean();
    final AtomicInteger counter = new AtomicInteger();
    final AtomicBoolean successExecuted = new AtomicBoolean();
    final Class<T> resultClass;
    final int timeout;

    public BroadcastAckCallback(Class<T> resultClass, int timeout) {
        this.resultClass = resultClass;
        this.timeout = timeout;
    }

    public BroadcastAckCallback(Class<T> resultClass) {
        this(resultClass, -1);
    }

    final AckCallback<T> createClientCallback(final SocketIOClient client) {
        counter.getAndIncrement();
        return new AckCallback<T>(resultClass, timeout) {
            @Override
            public void onSuccess(T result) {
                counter.getAndDecrement();
                onClientSuccess(client, result);
                executeSuccess();
            }

            @Override
            public void onTimeout() {
                onClientTimeout(client);
            }

        };
    }

    protected void onClientTimeout(SocketIOClient client) {

    }

    protected void onClientSuccess(SocketIOClient client, T result) {

    }

    protected void onAllSuccess() {

    }

    private void executeSuccess() {
        if (loopFinished.get()
                && counter.get() == 0
                    && successExecuted.compareAndSet(false, true)) {
            onAllSuccess();
        }
    }

    void loopFinished() {
        loopFinished.set(true);
        executeSuccess();
    }

}

