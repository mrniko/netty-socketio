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
package com.corundumstudio.socketio.quarkus.recorder;

import java.io.InputStream;

import javax.net.ssl.KeyManagerFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.AuthorizationListener;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.HttpRequestDecoderConfiguration;
import com.corundumstudio.socketio.SocketConfig;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.SocketSslConfig;
import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.listener.ExceptionListener;
import com.corundumstudio.socketio.protocol.JsonSupport;
import com.corundumstudio.socketio.quarkus.config.NettySocketIOBasicConfigMapping;
import com.corundumstudio.socketio.quarkus.config.NettySocketIOHttpRequestDecoderConfigMapping;
import com.corundumstudio.socketio.quarkus.config.NettySocketIOSocketConfigMapping;
import com.corundumstudio.socketio.quarkus.config.NettySocketIOSslConfigMapping;
import com.corundumstudio.socketio.store.StoreFactory;

import io.quarkus.arc.Arc;
import io.quarkus.arc.InstanceHandle;
import io.quarkus.runtime.RuntimeValue;
import io.quarkus.runtime.annotations.Recorder;

/**
 * Recorder class for Netty Socket.IO configuration.
 * Used to create and configure the SocketIOServer instance at runtime.
 */
@Recorder
public class NettySocketIOConfigRecorder {
    private static final Logger log = LoggerFactory.getLogger(NettySocketIOConfigRecorder.class);
    private final NettySocketIOBasicConfigMapping nettySocketIOBasicConfigMapping;
    private final NettySocketIOHttpRequestDecoderConfigMapping nettySocketIOHttpRequestDecoderConfigMapping;
    private final NettySocketIOSocketConfigMapping nettySocketIOSocketConfigMapping;
    private final NettySocketIOSslConfigMapping nettySocketIOSslConfigMapping;

    public NettySocketIOConfigRecorder(
            NettySocketIOBasicConfigMapping nettySocketIOBasicConfigMapping,
            NettySocketIOHttpRequestDecoderConfigMapping nettySocketIOHttpRequestDecoderConfigMapping,
            NettySocketIOSocketConfigMapping nettySocketIOSocketConfigMapping,
            NettySocketIOSslConfigMapping nettySocketIOSslConfigMapping
    ) {
        this.nettySocketIOBasicConfigMapping = nettySocketIOBasicConfigMapping;
        this.nettySocketIOHttpRequestDecoderConfigMapping = nettySocketIOHttpRequestDecoderConfigMapping;
        this.nettySocketIOSocketConfigMapping = nettySocketIOSocketConfigMapping;
        this.nettySocketIOSslConfigMapping = nettySocketIOSslConfigMapping;
    }

    public RuntimeValue<SocketIOServer> createSocketIOServer() {
        Configuration configuration = new Configuration();

        // Basic Config
        configuration.setContext(nettySocketIOBasicConfigMapping.context());
        configuration.setTransports(nettySocketIOBasicConfigMapping.transports().toArray(new Transport[0]));
        configuration.setBossThreads(nettySocketIOBasicConfigMapping.bossThreads());
        configuration.setWorkerThreads(nettySocketIOBasicConfigMapping.workerThreads());
        configuration.setUseLinuxNativeEpoll(nettySocketIOBasicConfigMapping.useLinuxNativeEpoll());
        configuration.setAllowCustomRequests(nettySocketIOBasicConfigMapping.allowCustomRequests());
        configuration.setUpgradeTimeout(nettySocketIOBasicConfigMapping.upgradeTimeout());
        configuration.setPingTimeout(nettySocketIOBasicConfigMapping.pingTimeout());
        configuration.setPingInterval(nettySocketIOBasicConfigMapping.pingInterval());
        configuration.setFirstDataTimeout(nettySocketIOBasicConfigMapping.firstDataTimeout());
        configuration.setMaxHttpContentLength(nettySocketIOBasicConfigMapping.maxHttpContentLength());
        configuration.setMaxFramePayloadLength(nettySocketIOBasicConfigMapping.maxFramePayloadLength());
        configuration.setPackagePrefix(nettySocketIOBasicConfigMapping.packagePrefix().orElse(null));
        configuration.setHostname(nettySocketIOBasicConfigMapping.hostname().orElse(null));
        configuration.setPort(nettySocketIOBasicConfigMapping.port());
        configuration.setAllowHeaders(nettySocketIOBasicConfigMapping.allowHeaders().orElse(null));
        configuration.setPreferDirectBuffer(nettySocketIOBasicConfigMapping.preferDirectBuffer());
        configuration.setAckMode(nettySocketIOBasicConfigMapping.ackMode());
        configuration.setAddVersionHeader(nettySocketIOBasicConfigMapping.addVersionHeader());
        configuration.setOrigin(nettySocketIOBasicConfigMapping.origin().orElse(null));
        configuration.setEnableCors(nettySocketIOBasicConfigMapping.enableCors());
        configuration.setHttpCompression(nettySocketIOBasicConfigMapping.httpCompression());
        configuration.setWebsocketCompression(nettySocketIOBasicConfigMapping.websocketCompression());
        configuration.setRandomSession(nettySocketIOBasicConfigMapping.randomSession());
        configuration.setNeedClientAuth(nettySocketIOBasicConfigMapping.needClientAuth());

        HttpRequestDecoderConfiguration requestDecoderConfiguration = new HttpRequestDecoderConfiguration();
        requestDecoderConfiguration.setMaxInitialLineLength(nettySocketIOHttpRequestDecoderConfigMapping.maxInitialLineLength());
        requestDecoderConfiguration.setMaxHeaderSize(nettySocketIOHttpRequestDecoderConfigMapping.maxHeaderSize());
        requestDecoderConfiguration.setMaxChunkSize(nettySocketIOHttpRequestDecoderConfigMapping.maxChunkSize());
        configuration.setHttpRequestDecoderConfiguration(requestDecoderConfiguration);

        SocketConfig socketConfig = new SocketConfig();
        socketConfig.setTcpNoDelay(nettySocketIOSocketConfigMapping.tcpNoDelay());
        socketConfig.setTcpSendBufferSize(nettySocketIOSocketConfigMapping.tcpSendBufferSize());
        socketConfig.setTcpReceiveBufferSize(nettySocketIOSocketConfigMapping.tcpReceiveBufferSize());
        socketConfig.setTcpKeepAlive(nettySocketIOSocketConfigMapping.tcpKeepAlive());
        socketConfig.setSoLinger(nettySocketIOSocketConfigMapping.soLinger());
        socketConfig.setReuseAddress(nettySocketIOSocketConfigMapping.reuseAddress());
        socketConfig.setAcceptBackLog(nettySocketIOSocketConfigMapping.acceptBackLog());
        socketConfig.setWriteBufferWaterMarkLow(nettySocketIOSocketConfigMapping.writeBufferWaterMarkLow());
        socketConfig.setWriteBufferWaterMarkHigh(nettySocketIOSocketConfigMapping.writeBufferWaterMarkHigh());
        configuration.setSocketConfig(socketConfig);

        SocketSslConfig sslConfig = new SocketSslConfig();
        sslConfig.setSSLProtocol(nettySocketIOSslConfigMapping.sslProtocol());
        sslConfig.setKeyStoreFormat(nettySocketIOSslConfigMapping.keyStoreFormat());
        if (nettySocketIOSslConfigMapping.keyStore() != null && nettySocketIOSslConfigMapping.keyStore().isPresent()) {
            InputStream keyStoreStream = getClass().getClassLoader()
                    .getResourceAsStream(nettySocketIOSslConfigMapping.keyStore().get());
            sslConfig.setKeyStore(keyStoreStream);
        }
        sslConfig.setKeyStorePassword(nettySocketIOSslConfigMapping.keyStorePassword().orElse(null));
        sslConfig.setTrustStoreFormat(nettySocketIOSslConfigMapping.trustStoreFormat());
        if (nettySocketIOSslConfigMapping.trustStore() != null && nettySocketIOSslConfigMapping.trustStore().isPresent()) {
            InputStream trustStoreStream = getClass().getClassLoader()
                    .getResourceAsStream(nettySocketIOSslConfigMapping.trustStore().get());
            sslConfig.setTrustStore(trustStoreStream);
        }
        sslConfig.setTrustStorePassword(nettySocketIOSslConfigMapping.trustStorePassword().orElse(null));
        sslConfig.setKeyManagerFactoryAlgorithm(nettySocketIOSslConfigMapping.keyManagerFactoryAlgorithm().orElse(
                KeyManagerFactory.getDefaultAlgorithm()
        ));
        configuration.setSocketSslConfig(sslConfig);

        InstanceHandle<ExceptionListener> exceptionListenerInstanceHandle = Arc.container().instance(ExceptionListener.class);
        if (exceptionListenerInstanceHandle != null && exceptionListenerInstanceHandle.isAvailable()) {
            log.info("Netty socket-io server configuration uses ExceptionListener: {}", exceptionListenerInstanceHandle.get().getClass().getName());
            configuration.setExceptionListener(exceptionListenerInstanceHandle.get());
        }

        InstanceHandle<StoreFactory> storeFactoryInstanceHandle = Arc.container().instance(StoreFactory.class);
        if (storeFactoryInstanceHandle != null && storeFactoryInstanceHandle.isAvailable()) {
            log.info("Netty socket-io server configuration uses StoreFactory: {}", storeFactoryInstanceHandle.get().getClass().getName());
            configuration.setStoreFactory(storeFactoryInstanceHandle.get());
        }

        InstanceHandle<JsonSupport> jsonSupportInstanceHandle = Arc.container().instance(JsonSupport.class);
        if (jsonSupportInstanceHandle != null && jsonSupportInstanceHandle.isAvailable()) {
            log.info("Netty socket-io server configuration uses JsonSupport: {}", jsonSupportInstanceHandle.get().getClass().getName());
            configuration.setJsonSupport(jsonSupportInstanceHandle.get());
        }

        InstanceHandle<AuthorizationListener> authorizationListenerInstanceHandle = Arc.container().instance(AuthorizationListener.class);
        if (authorizationListenerInstanceHandle != null && authorizationListenerInstanceHandle.isAvailable()) {
            log.info("Netty socket-io server configuration uses AuthorizationListener: {}", authorizationListenerInstanceHandle.get().getClass().getName());
            configuration.setAuthorizationListener(authorizationListenerInstanceHandle.get());
        }

        return new RuntimeValue<>(new SocketIOServer(configuration));
    }
}
