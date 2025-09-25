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
package com.corundumstudio.socketio.test.spring.boot.starter.config;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.HttpRequestDecoderConfiguration;
import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketSslConfig;
import com.corundumstudio.socketio.spring.boot.starter.config.NettySocketIOBasicConfigurationProperties;
import com.corundumstudio.socketio.spring.boot.starter.config.NettySocketIOHttpRequestDecoderConfigurationProperties;
import com.corundumstudio.socketio.spring.boot.starter.config.NettySocketIOSocketConfigProperties;
import com.corundumstudio.socketio.spring.boot.starter.config.NettySocketIOSslConfigProperties;
import com.corundumstudio.socketio.test.spring.boot.starter.BaseSpringApplicationTest;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@DisplayName("Test for Socket.IO configuration properties")
public class SocketIOOriginConfigurationTest extends BaseSpringApplicationTest {
    private static final int PORT = 9090;
    private static final int MAX_HEADER_SIZE = 1024;
    private static final boolean TCP_KEEP_ALIVE = true;

    @Autowired
    private NettySocketIOBasicConfigurationProperties nettySocketIOBasicConfigurationProperties;
    @Autowired
    private NettySocketIOHttpRequestDecoderConfigurationProperties
            nettySocketIOHttpRequestDecoderConfigurationProperties;
    @Autowired
    private NettySocketIOSocketConfigProperties nettySocketIOSocketConfigProperties;
    @Autowired
    private NettySocketIOSslConfigProperties nettySocketIOSslConfigProperties;

    @DynamicPropertySource
    public static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("netty-socket-io.port", () -> PORT);
        registry.add("netty-socket-io.http-request-decoder.max-header-size", () -> MAX_HEADER_SIZE);
        registry.add("netty-socket-io.socket.tcp-keep-alive", () -> TCP_KEEP_ALIVE);
        registry.add("netty-socket-io.ssl.key-store", () -> "classpath:keystore.jks");
        registry.add("netty-socket-io.ssl.key-store-password", () -> "test123456");
    }

    @Test
    @DisplayName("Test basic configuration properties")
    public void testBasicConfigurationProperties() {
        Configuration configuration = new Configuration();

        //only port is changed
        assertEquals(PORT, nettySocketIOBasicConfigurationProperties.getPort());

        // Basic configuration properties
        assertEquals(nettySocketIOBasicConfigurationProperties.getContext(), configuration.getContext());
        assertEquals(nettySocketIOBasicConfigurationProperties.getTransports(), configuration.getTransports());
        assertEquals(nettySocketIOBasicConfigurationProperties.getBossThreads(), configuration.getBossThreads());
        assertEquals(nettySocketIOBasicConfigurationProperties.getWorkerThreads(), configuration.getWorkerThreads());
        assertEquals(nettySocketIOBasicConfigurationProperties.isUseLinuxNativeEpoll(), configuration.isUseLinuxNativeEpoll());
        assertEquals(nettySocketIOBasicConfigurationProperties.isAllowCustomRequests(), configuration.isAllowCustomRequests());

        // Timeout configurations
        assertEquals(nettySocketIOBasicConfigurationProperties.getUpgradeTimeout(), configuration.getUpgradeTimeout());
        assertEquals(nettySocketIOBasicConfigurationProperties.getPingTimeout(), configuration.getPingTimeout());
        assertEquals(nettySocketIOBasicConfigurationProperties.getPingInterval(), configuration.getPingInterval());
        assertEquals(nettySocketIOBasicConfigurationProperties.getFirstDataTimeout(), configuration.getFirstDataTimeout());

        // Content length configurations
        assertEquals(nettySocketIOBasicConfigurationProperties.getMaxHttpContentLength(), configuration.getMaxHttpContentLength());
        assertEquals(nettySocketIOBasicConfigurationProperties.getMaxFramePayloadLength(), configuration.getMaxFramePayloadLength());

        // Network configurations
        assertEquals(nettySocketIOBasicConfigurationProperties.getPackagePrefix(), configuration.getPackagePrefix());
        assertEquals(nettySocketIOBasicConfigurationProperties.getHostname(), configuration.getHostname());
        assertEquals(nettySocketIOBasicConfigurationProperties.getAllowHeaders(), configuration.getAllowHeaders());

        // Buffer and performance configurations
        assertEquals(nettySocketIOBasicConfigurationProperties.isPreferDirectBuffer(), configuration.isPreferDirectBuffer());
        assertEquals(nettySocketIOBasicConfigurationProperties.getAckMode(), configuration.getAckMode());

        // Header and CORS configurations
        assertEquals(nettySocketIOBasicConfigurationProperties.isAddVersionHeader(), configuration.isAddVersionHeader());
        assertEquals(nettySocketIOBasicConfigurationProperties.getOrigin(), configuration.getOrigin());
        assertEquals(nettySocketIOBasicConfigurationProperties.isEnableCors(), configuration.isEnableCors());

        // Compression configurations
        assertEquals(nettySocketIOBasicConfigurationProperties.isHttpCompression(), configuration.isHttpCompression());
        assertEquals(nettySocketIOBasicConfigurationProperties.isWebsocketCompression(), configuration.isWebsocketCompression());

        // Session and authentication configurations
        assertEquals(nettySocketIOBasicConfigurationProperties.isRandomSession(), configuration.isRandomSession());
        assertEquals(nettySocketIOBasicConfigurationProperties.isNeedClientAuth(), configuration.isNeedClientAuth());
    }

    @Test
    @DisplayName("Test HTTP request decoder configuration properties")
    public void testHttpRequestDecoderConfigurationProperties() {
        HttpRequestDecoderConfiguration httpRequestDecoderConfiguration = new HttpRequestDecoderConfiguration();
        assertEquals(nettySocketIOHttpRequestDecoderConfigurationProperties.getMaxInitialLineLength(),
                httpRequestDecoderConfiguration.getMaxInitialLineLength());
        assertEquals(nettySocketIOHttpRequestDecoderConfigurationProperties.getMaxChunkSize(),
                httpRequestDecoderConfiguration.getMaxChunkSize());
        // only maxHeaderSize is changed
        assertEquals(MAX_HEADER_SIZE,
                nettySocketIOHttpRequestDecoderConfigurationProperties.getMaxHeaderSize());
    }

    @Test
    @DisplayName("Test Socket configuration properties")
    public void testSocketConfigProperties() {
        // only tcpKeepAlive is changed
        assertEquals(TCP_KEEP_ALIVE, nettySocketIOSocketConfigProperties.isTcpKeepAlive());
        SocketConfig socketConfig = new SocketConfig();
        assertEquals(nettySocketIOSocketConfigProperties.isTcpNoDelay(),
                socketConfig.isTcpNoDelay());
        assertEquals(nettySocketIOSocketConfigProperties.getTcpSendBufferSize(),
                socketConfig.getTcpSendBufferSize());
        assertEquals(nettySocketIOSocketConfigProperties.getTcpReceiveBufferSize(),
                socketConfig.getTcpReceiveBufferSize());
        assertEquals(nettySocketIOSocketConfigProperties.getSoLinger(),
                socketConfig.getSoLinger());
        assertEquals(nettySocketIOSocketConfigProperties.isReuseAddress(),
                socketConfig.isReuseAddress());
        assertEquals(nettySocketIOSocketConfigProperties.getAcceptBackLog(),
                socketConfig.getAcceptBackLog());
        assertEquals(nettySocketIOSocketConfigProperties.getWriteBufferWaterMarkLow(),
                socketConfig.getWriteBufferWaterMarkLow());
        assertEquals(nettySocketIOSocketConfigProperties.getWriteBufferWaterMarkHigh(),
                socketConfig.getWriteBufferWaterMarkHigh());
    }

    @Test
    @DisplayName("Test SSL configuration properties")
    public void testSslConfigProperties() {
        assertNotNull(nettySocketIOSslConfigProperties.getKeyStore(), "Key store should be loaded");
        assertNotNull(nettySocketIOSslConfigProperties.getKeyStorePassword(), "Key store password should be loaded");

        SocketSslConfig socketSslConfig = new SocketSslConfig();
        assertEquals(nettySocketIOSslConfigProperties.getTrustStore(),
                socketSslConfig.getTrustStore());
        assertEquals(nettySocketIOSslConfigProperties.getTrustStorePassword(),
                socketSslConfig.getTrustStorePassword());
        assertEquals(nettySocketIOSslConfigProperties.getKeyStoreFormat(),
                socketSslConfig.getKeyStoreFormat());
        assertEquals(nettySocketIOSslConfigProperties.getTrustStoreFormat(),
                socketSslConfig.getTrustStoreFormat());
        assertEquals(nettySocketIOSslConfigProperties.getSSLProtocol(),
                socketSslConfig.getSSLProtocol());
        assertEquals(nettySocketIOSslConfigProperties.getKeyManagerFactoryAlgorithm(),
                socketSslConfig.getKeyManagerFactoryAlgorithm());
    }
}
