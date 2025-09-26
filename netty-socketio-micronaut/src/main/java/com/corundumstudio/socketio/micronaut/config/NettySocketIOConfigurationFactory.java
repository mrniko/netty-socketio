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

import com.corundumstudio.socketio.AuthorizationListener;
import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.SocketSslConfig;
import com.corundumstudio.socketio.handler.SuccessAuthorizationListener;
import com.corundumstudio.socketio.listener.DefaultExceptionListener;
import com.corundumstudio.socketio.listener.ExceptionListener;
import com.corundumstudio.socketio.micronaut.annotation.MicronautAnnotationScanner;
import com.corundumstudio.socketio.micronaut.lifecycle.NettySocketIOServerShutdown;
import com.corundumstudio.socketio.micronaut.lifecycle.NettySocketIOServerStartup;
import com.corundumstudio.socketio.protocol.JacksonJsonSupport;
import com.corundumstudio.socketio.protocol.JsonSupport;
import com.corundumstudio.socketio.store.MemoryStoreFactory;
import com.corundumstudio.socketio.store.StoreFactory;

import io.micronaut.context.BeanContext;
import io.micronaut.context.annotation.Factory;
import io.micronaut.context.annotation.Primary;
import io.micronaut.context.annotation.Requires;
import jakarta.inject.Singleton;

/**
 * Configuration factory for Netty Socket.IO server in Micronaut.
 * This factory provides all necessary beans for Socket.IO server configuration.
 */
@Factory
public class NettySocketIOConfigurationFactory {

    @Singleton
    @Primary
    public Configuration nettySocketIOConfiguration(
            NettySocketIOBasicConfigurationProperties properties,
            ExceptionListener exceptionListener,
            NettySocketIOSocketConfigProperties nettySocketIOSocketConfigProperties,
            StoreFactory storeFactory,
            JsonSupport jsonSupport,
            AuthorizationListener authorizationListener,
            NettySocketIOHttpRequestDecoderConfigurationProperties nettySocketIOHttpRequestDecoderConfigurationProperties,
            NettySocketIOSslConfigProperties nettySocketIOSslConfigProperties
    ) {
        Configuration configuration = new Configuration(properties);
        configuration.setExceptionListener(exceptionListener);
        configuration.setSocketConfig(nettySocketIOSocketConfigProperties);
        configuration.setStoreFactory(storeFactory);
        configuration.setJsonSupport(jsonSupport);
        configuration.setAuthorizationListener(authorizationListener);
        configuration.setHttpRequestDecoderConfiguration(nettySocketIOHttpRequestDecoderConfigurationProperties);

        SocketSslConfig socketSslConfig = new SocketSslConfig();
        socketSslConfig.setSSLProtocol(nettySocketIOSslConfigProperties.getSslProtocol());
        if (nettySocketIOSslConfigProperties.getKeyStore() != null) {
            socketSslConfig.setKeyStore(
                    this.getClass().getResourceAsStream(nettySocketIOSslConfigProperties.getKeyStore())
            );
        }
        socketSslConfig.setKeyStorePassword(nettySocketIOSslConfigProperties.getKeyStorePassword());
        socketSslConfig.setKeyStoreFormat(nettySocketIOSslConfigProperties.getKeyStoreFormat());
        if (nettySocketIOSslConfigProperties.getTrustStore() != null) {
            socketSslConfig.setTrustStore(
                    this.getClass().getResourceAsStream(nettySocketIOSslConfigProperties.getTrustStore())
            );
        }
        socketSslConfig.setTrustStorePassword(nettySocketIOSslConfigProperties.getTrustStorePassword());
        socketSslConfig.setTrustStoreFormat(nettySocketIOSslConfigProperties.getTrustStoreFormat());
        socketSslConfig.setKeyManagerFactoryAlgorithm(nettySocketIOSslConfigProperties.getKeyManagerFactoryAlgorithm());
        configuration.setSocketSslConfig(socketSslConfig);

        return configuration;
    }

    @Singleton
    @Requires(missingBeans = ExceptionListener.class)
    public ExceptionListener nettySocketIOExceptionListener() {
        return new DefaultExceptionListener();
    }

    @Singleton
    @Requires(missingBeans = StoreFactory.class)
    public StoreFactory nettySocketIOStoreFactory() {
        return new MemoryStoreFactory();
    }

    @Singleton
    @Requires(missingBeans = JsonSupport.class)
    public JsonSupport nettySocketIOJsonSupport() {
        return new JacksonJsonSupport();
    }

    @Singleton
    @Requires(missingBeans = AuthorizationListener.class)
    public AuthorizationListener nettySocketIOAuthorizationListener() {
        return new SuccessAuthorizationListener();
    }

    @Singleton
    public SocketIOServer socketIOServer(Configuration configuration) {
        return new SocketIOServer(configuration);
    }

    @Singleton
    public NettySocketIOServerStartup nettySocketIOServerStartup(SocketIOServer socketIOServer, MicronautAnnotationScanner micronautAnnotationScanner) {
        return new NettySocketIOServerStartup(socketIOServer, micronautAnnotationScanner);
    }

    @Singleton
    public NettySocketIOServerShutdown nettySocketIOServerShutdown(SocketIOServer socketIOServer) {
        return new NettySocketIOServerShutdown(socketIOServer);
    }

    @Singleton
    public MicronautAnnotationScanner micronautAnnotationScanner(BeanContext beanContext, SocketIOServer socketIOServer) {
        return new MicronautAnnotationScanner(beanContext, socketIOServer);
    }
}
