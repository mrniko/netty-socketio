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

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import com.corundumstudio.socketio.AuthorizationListener;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.handler.SuccessAuthorizationListener;
import com.corundumstudio.socketio.listener.DefaultExceptionListener;
import com.corundumstudio.socketio.listener.ExceptionListener;
import com.corundumstudio.socketio.protocol.JacksonJsonSupport;
import com.corundumstudio.socketio.protocol.JsonSupport;
import com.corundumstudio.socketio.spring.SpringAnnotationScanner;
import com.corundumstudio.socketio.spring.boot.starter.lifecycle.NettySocketIOLifecycle;
import com.corundumstudio.socketio.store.MemoryStoreFactory;
import com.corundumstudio.socketio.store.StoreFactory;

@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties({
        NettySocketIOBasicConfigurationProperties.class,
        NettySocketIOSocketConfigProperties.class,
        NettySocketIOHttpRequestDecoderConfigurationProperties.class
})
public class NettySocketIODefaultConfiguration {
    @Bean
    public com.corundumstudio.socketio.Configuration nettySocketIOConfiguration(
            NettySocketIOBasicConfigurationProperties properties,
            ExceptionListener exceptionListener,
            NettySocketIOSocketConfigProperties nettySocketIOSocketConfigProperties,
            StoreFactory storeFactory,
            JsonSupport jsonSupport,
            AuthorizationListener authorizationListener,
            NettySocketIOHttpRequestDecoderConfigurationProperties nettySocketIOHttpRequestDecoderConfigurationProperties
    ) {
        com.corundumstudio.socketio.Configuration configuration = new com.corundumstudio.socketio.Configuration(properties);
        configuration.setExceptionListener(exceptionListener);
        configuration.setSocketConfig(nettySocketIOSocketConfigProperties);
        configuration.setStoreFactory(storeFactory);
        configuration.setJsonSupport(jsonSupport);
        configuration.setAuthorizationListener(authorizationListener);
        configuration.setHttpRequestDecoderConfiguration(nettySocketIOHttpRequestDecoderConfigurationProperties);
        return configuration;
    }

    @Bean
    @ConditionalOnMissingBean
    public ExceptionListener nettySocketIOExceptionListener() {
        return new DefaultExceptionListener();
    }

    @Bean
    @ConditionalOnMissingBean
    public StoreFactory nettySocketIOStoreFactory() {
        return new MemoryStoreFactory();
    }

    @Bean
    @ConditionalOnMissingBean
    public JsonSupport nettySocketIOJsonSupport() {
        return new JacksonJsonSupport();
    }

    @Bean
    @ConditionalOnMissingBean
    public AuthorizationListener nettySocketIOAuthorizationListener() {
        return new SuccessAuthorizationListener();
    }

    @Bean
    public SocketIOServer socketIOServer(com.corundumstudio.socketio.Configuration configuration) {
        return new SocketIOServer(configuration);
    }

    @Bean
    public NettySocketIOLifecycle nettySocketIOLifecycle(
            NettySocketIOBasicConfigurationProperties nettySocketIOBasicConfigurationProperties,
            SocketIOServer socketIOServer
    ) {
        return new NettySocketIOLifecycle(nettySocketIOBasicConfigurationProperties, socketIOServer);
    }

    @Bean
    @ConditionalOnMissingBean
    public SpringAnnotationScanner nettySocketIOAnnotationScanner(SocketIOServer socketIOServer) {
        return new SpringAnnotationScanner(socketIOServer);
    }
}
