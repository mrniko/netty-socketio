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
package com.corundumstudio.socketio.micronaut.config;

import com.corundumstudio.socketio.SocketConfig;

import io.micronaut.context.annotation.ConfigurationProperties;

import static com.corundumstudio.socketio.micronaut.config.NettySocketIOSocketConfigProperties.PREFIX;

/**
 * Socket configuration properties for Netty Socket.IO server in Micronaut.
 * @see com.corundumstudio.socketio.SocketConfig
 */
@ConfigurationProperties(PREFIX)
public class NettySocketIOSocketConfigProperties extends SocketConfig {
    public static final String PREFIX = "netty-socket-io.socket";
}
