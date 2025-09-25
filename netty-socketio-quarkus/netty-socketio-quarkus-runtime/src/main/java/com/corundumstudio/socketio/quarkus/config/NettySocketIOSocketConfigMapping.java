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
package com.corundumstudio.socketio.quarkus.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration properties for Netty SocketIO socket in Quarkus application.
 * These properties can be set in the application.properties file with the prefix "netty-socketio.socket".
 *
 * @see com.corundumstudio.socketio.SocketConfig
 */
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "netty-socketio.socket")
public interface NettySocketIOSocketConfigMapping {
    /**
     * TCP SO
     * @see com.corundumstudio.socketio.SocketConfig#getSoLinger()
     */
    @WithDefault("-1")
    int soLinger();

    /**
     * tcpNoDelay option
     * @see com.corundumstudio.socketio.SocketConfig#isTcpNoDelay()
     */
    @WithDefault("true")
    boolean tcpNoDelay();

    /**
     * tcp send buffer size
     * @see com.corundumstudio.socketio.SocketConfig#getTcpSendBufferSize()
     */
    @WithDefault("-1")
    int tcpSendBufferSize();

    /**
     * tcp receive buffer size
     * @see com.corundumstudio.socketio.SocketConfig#getTcpReceiveBufferSize()
     */
    @WithDefault("-1")
    int tcpReceiveBufferSize();

    /**
     * tcp keep alive option
     * @see com.corundumstudio.socketio.SocketConfig#isTcpKeepAlive()
     */
    @WithDefault("false")
    boolean tcpKeepAlive();

    /**
     * enable address reuse
     * @see com.corundumstudio.socketio.SocketConfig#isReuseAddress()
     */
    @WithDefault("false")
    boolean reuseAddress();

    /**
     * accept backlog
     * @see com.corundumstudio.socketio.SocketConfig#getAcceptBackLog()
     */
    @WithDefault("1024")
    int acceptBackLog();

    /**
     * write buffer watermark low
     * @see com.corundumstudio.socketio.SocketConfig#getWriteBufferWaterMarkLow()
     */
    @WithDefault("-1")
    int writeBufferWaterMarkLow();

    /**
     * write buffer watermark high
     * @see com.corundumstudio.socketio.SocketConfig#getWriteBufferWaterMarkHigh()
     */
    @WithDefault("-1")
    int writeBufferWaterMarkHigh();
}
