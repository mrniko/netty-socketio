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
package com.corundumstudio.socketio.micronaut.lifecycle;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.SocketIOServer;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.event.ApplicationShutdownEvent;

/**
 * Listener to stop the Netty Socket.IO server when the Micronaut application shutdown.
 */
public class NettySocketIOServerShutdown implements ApplicationEventListener<ApplicationShutdownEvent> {

    private static final Logger log = LoggerFactory.getLogger(NettySocketIOServerShutdown.class);
    private final SocketIOServer socketIOServer;

    public NettySocketIOServerShutdown(SocketIOServer socketIOServer) {
        this.socketIOServer = socketIOServer;
    }

    @Override
    public void onApplicationEvent(ApplicationShutdownEvent event) {
        log.info("Shutting down Netty SocketIOServer");
        socketIOServer.stop();
        log.info("Netty SocketIOServer shut down");
    }
}
