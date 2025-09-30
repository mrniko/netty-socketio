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
package com.corundumstudio.socketio.spring.boot.starter.lifecycle;

import org.springframework.context.SmartLifecycle;

import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.spring.boot.starter.config.NettySocketIOBasicConfigurationProperties;

/**
 * Lifecycle management for Netty Socket.IO server.
 * This class implements SmartLifecycle to manage the start and stop of the SocketIOServer
 * based on the application context lifecycle.
 */
public class NettySocketIOLifecycle implements SmartLifecycle {

    private final NettySocketIOBasicConfigurationProperties nettySocketIOBasicConfigurationProperties;
    private final SocketIOServer socketIOServer;

    public NettySocketIOLifecycle(NettySocketIOBasicConfigurationProperties nettySocketIOBasicConfigurationProperties, SocketIOServer socketIOServer) {
        this.nettySocketIOBasicConfigurationProperties = nettySocketIOBasicConfigurationProperties;
        this.socketIOServer = socketIOServer;
    }

    @Override
    public int getPhase() {
        return nettySocketIOBasicConfigurationProperties.getServerLifeCyclePhase();
    }

    @Override
    public void start() {
        socketIOServer.start();
    }

    @Override
    public void stop() {
        socketIOServer.stop();
    }

    @Override
    public boolean isRunning() {
        return socketIOServer.isStarted();
    }
}
