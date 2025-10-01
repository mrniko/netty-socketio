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
package com.corundumstudio.socketio.spring.boot.starter.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import com.corundumstudio.socketio.BasicConfiguration;

import static com.corundumstudio.socketio.spring.boot.starter.config.NettySocketIOBasicConfigurationProperties.PREFIX;

/**
 * Basic configuration properties for Netty Socket.IO server.
 * This class extends BasicConfiguration
 * But for default values, refer to the following classes' constructors:
 * @see com.corundumstudio.socketio.BasicConfiguration
 * @see com.corundumstudio.socketio.Configuration
 */
@ConfigurationProperties(prefix = PREFIX)
public class NettySocketIOBasicConfigurationProperties extends BasicConfiguration {
    public static final String PREFIX = "netty-socket-io";

    /**
     * The order of the server lifecycle. Default is 0.
     * You can set a negative value to start the server before other lifecycle beans.
     * @see org.springframework.context.SmartLifecycle#getPhase()
     */
    private int serverLifeCyclePhase = 0;

    public int getServerLifeCyclePhase() {
        return serverLifeCyclePhase;
    }

    public void setServerLifeCyclePhase(int serverLifeCyclePhase) {
        this.serverLifeCyclePhase = serverLifeCyclePhase;
    }
}
