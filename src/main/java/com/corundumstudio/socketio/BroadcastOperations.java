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
package com.corundumstudio.socketio;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import com.corundumstudio.socketio.parser.Packet;

public class BroadcastOperations implements ClientOperations {

    private class BroadcastAckCallback extends AckCallback {

        final AtomicBoolean loopFinished = new AtomicBoolean();
        final AtomicInteger counter = new AtomicInteger();
        final AtomicBoolean timeoutExecuted = new AtomicBoolean();
        final AtomicBoolean successExecuted = new AtomicBoolean();
        final AckCallback ackCallback;

        public BroadcastAckCallback(AckCallback ackCallback) {
            super(Void.class);
            this.ackCallback = ackCallback;
        }

        @Override
        public void onSuccess(Object result) {
            counter.getAndDecrement();
            executeSuccess();
        }

        private void executeSuccess() {
            if (loopFinished.get() && counter.get() == 0 && successExecuted.compareAndSet(false, true)) {
                ackCallback.onSuccess(null);
            }
        }

        @Override
        public void onTimeout() {
            // execute onTimeout once
            if (timeoutExecuted.compareAndSet(false, true)) {
                ackCallback.onTimeout();
            }
        }

        void incrementCounter() {
            counter.getAndIncrement();
        }

        void loopFinished() {
            loopFinished.set(true);
            executeSuccess();
        }

    }

    private final Iterable<SocketIOClient> clients;

    public BroadcastOperations(Iterable<SocketIOClient> clients) {
        super();
        this.clients = clients;
    }

    @Override
    public void sendMessage(String message) {
        for (SocketIOClient client : clients) {
            client.sendMessage(message);
        }
    }

    @Override
    public void sendMessage(String message, AckCallback ackCallback) {
        BroadcastAckCallback clientCallback = new BroadcastAckCallback(ackCallback);
        for (SocketIOClient client : clients) {
            clientCallback.incrementCounter();
            client.sendMessage(message, clientCallback);
        }
        clientCallback.loopFinished();
    }

    @Override
    public void sendJsonObject(Object object) {
        for (SocketIOClient client : clients) {
            client.sendJsonObject(object);
        }
    }

    @Override
    public void sendJsonObject(Object object, AckCallback ackCallback) {
        BroadcastAckCallback clientCallback = new BroadcastAckCallback(ackCallback);
        for (SocketIOClient client : clients) {
            clientCallback.incrementCounter();
            client.sendJsonObject(object, clientCallback);
        }
        clientCallback.loopFinished();
    }

    @Override
    public void send(Packet packet) {
        for (SocketIOClient client : clients) {
            client.send(packet);
        }
    }

    @Override
    public void send(Packet packet, AckCallback ackCallback) {
        BroadcastAckCallback clientCallback = new BroadcastAckCallback(ackCallback);
        for (SocketIOClient client : clients) {
            clientCallback.incrementCounter();
            client.send(packet, ackCallback);
        }
        clientCallback.loopFinished();
    }

    @Override
    public void disconnect() {
        for (SocketIOClient client : clients) {
            client.disconnect();
        }
    }

    @Override
    public void sendEvent(String name, Object data) {
        for (SocketIOClient client : clients) {
            client.sendEvent(name, data);
        }
    }

    @Override
    public void sendEvent(String name, Object data, AckCallback ackCallback) {
        BroadcastAckCallback clientCallback = new BroadcastAckCallback(ackCallback);
        for (SocketIOClient client : clients) {
            clientCallback.incrementCounter();
            client.sendEvent(name, data, ackCallback);
        }
        clientCallback.loopFinished();
    }

}
