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


import java.util.List;
import java.util.Optional;

import com.corundumstudio.socketio.AckMode;
import com.corundumstudio.socketio.BasicConfiguration;
import com.corundumstudio.socketio.Transport;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration properties for Netty SocketIO server in Quarkus application.
 * These properties can be set in the application.properties file with the prefix "netty-socketio".
 *
 * @see com.corundumstudio.socketio.BasicConfiguration
 */
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "netty-socketio")
public interface NettySocketIOBasicConfigMapping {
    /**
     * context path
     * @see BasicConfiguration#getContext()
     */
    @WithDefault("/socket.io")
    String context();

    /**
     * Engine.IO protocol version
     * @see BasicConfiguration#getTransports()
     */
    @WithDefault("websocket,polling")
    List<Transport> transports();

    /**
     * Supported Engine.IO protocol versions
     * @see BasicConfiguration#getBossThreads()
     */
    @WithDefault("0")
    int bossThreads();

    /**
     * Number of worker threads, defaults to number of available processors * 2
     * @see BasicConfiguration#getWorkerThreads()
     */
    @WithDefault("0")
    int workerThreads();

    /**
     * Enable use of Linux native epoll transport if available
     * @see BasicConfiguration#isUseLinuxNativeEpoll()
     */
    @WithDefault("false")
    boolean useLinuxNativeEpoll();

    @WithDefault("false")
    boolean useLinuxNativeIoUring();

    /**
     * Allow requests other than Engine.IO protocol
     * @see BasicConfiguration#isAllowCustomRequests()
     */
    @WithDefault("false")
    boolean allowCustomRequests();

    /**
     * upgrade timeout in milliseconds
     * @see BasicConfiguration#getUpgradeTimeout()
     */
    @WithDefault("10000")
    int upgradeTimeout();

    /**
     * ping timeout in milliseconds
     * @see BasicConfiguration#getPingTimeout()
     */
    @WithDefault("60000")
    int pingTimeout();

    /**
     * ping interval in milliseconds
     * @see BasicConfiguration#getPingInterval()
     */
    @WithDefault("25000")
    int pingInterval();

    /**
     * timeout for the first data packet from client in milliseconds
     * @see BasicConfiguration#getFirstDataTimeout()
     */
    @WithDefault("5000")
    int firstDataTimeout();

    /**
     * max http content length
     * @see BasicConfiguration#getMaxHttpContentLength()
     */
    @WithDefault("65536")
    int maxHttpContentLength();

    /**
     * max websocket frame payload length
     * @see BasicConfiguration#getMaxFramePayloadLength()
     */
    @WithDefault("65536")
    int maxFramePayloadLength();

    /**
     * WebSocket idle timeout in milliseconds
     * @see BasicConfiguration#getPackagePrefix()
     */
    Optional<String> packagePrefix();

    /**
     * hostname
     * @see BasicConfiguration#getHostname()
     */
    Optional<String> hostname();

    /**
     * port
     * @see BasicConfiguration#getPort()
     */
    @WithDefault("-1")
    int port();

    /**
     * allow headers
     * @see BasicConfiguration#getAllowHeaders()
     */
    Optional<String> allowHeaders();

    /**
     * prefer direct buffer for websocket frames
     * @see BasicConfiguration#isPreferDirectBuffer()
     */
    @WithDefault("true")
    boolean preferDirectBuffer();

    /**
     * ack mode
     * @see BasicConfiguration#getAckMode()
     */
    @WithDefault("AUTO_SUCCESS_ONLY")
    AckMode ackMode();

    /**
     * add version header
     * @see BasicConfiguration#isAddVersionHeader()
     */
    @WithDefault("true")
    boolean addVersionHeader();

    /**
     * origin
     * @see BasicConfiguration#getOrigin()
     */
    Optional<String> origin();

    /**
     * enable CORS
     * @see BasicConfiguration#isEnableCors()
     */
    @WithDefault("true")
    boolean enableCors();

    /**
     * http compression
     * @see BasicConfiguration#isHttpCompression()
     */
    @WithDefault("true")
    boolean httpCompression();

    /**
     * websocket compression
     * @see BasicConfiguration#isWebsocketCompression()
     */
    @WithDefault("true")
    boolean websocketCompression();

    /**
     * random session
     * @see BasicConfiguration#isRandomSession()
     */
    @WithDefault("false")
    boolean randomSession();

    /**
     * need client auth
     * @see BasicConfiguration#isNeedClientAuth()
     */
    @WithDefault("false")
    boolean needClientAuth();
}
