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
import com.corundumstudio.socketio.micronaut.annotation.MicronautAnnotationScanner;

import io.micronaut.context.event.ApplicationEventListener;
import io.micronaut.runtime.event.ApplicationStartupEvent;

/**
 * Listener to start the Netty Socket.IO server when the Micronaut application starts.
 */
public class NettySocketIOServerStartup implements ApplicationEventListener<ApplicationStartupEvent> {

    private static final Logger log = LoggerFactory.getLogger(NettySocketIOServerStartup.class);
    private final SocketIOServer socketIOServer;
    private final MicronautAnnotationScanner micronautAnnotationScanner;

    public NettySocketIOServerStartup(SocketIOServer socketIOServer, MicronautAnnotationScanner micronautAnnotationScanner) {
        this.socketIOServer = socketIOServer;
        this.micronautAnnotationScanner = micronautAnnotationScanner;
    }

    @Override
    public void onApplicationEvent(ApplicationStartupEvent event) {
        log.info("Starting up Netty SocketIOServer");
        micronautAnnotationScanner.scanAndRegister();
        socketIOServer.start();
        log.info("Netty SocketIOServer started");
    }
}
