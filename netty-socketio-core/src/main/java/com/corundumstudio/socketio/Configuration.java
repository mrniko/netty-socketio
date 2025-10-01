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
package com.corundumstudio.socketio;

import com.corundumstudio.socketio.handler.SuccessAuthorizationListener;
import com.corundumstudio.socketio.listener.DefaultExceptionListener;
import com.corundumstudio.socketio.listener.ExceptionListener;
import com.corundumstudio.socketio.protocol.JsonSupport;
import com.corundumstudio.socketio.store.MemoryStoreFactory;
import com.corundumstudio.socketio.store.StoreFactory;

import io.netty.handler.codec.http.HttpDecoderConfig;

public class Configuration extends BasicConfiguration {

    private ExceptionListener exceptionListener = new DefaultExceptionListener();

    private SocketConfig socketConfig = new SocketConfig();

    private SocketSslConfig socketSslConfig = new SocketSslConfig();

    private StoreFactory storeFactory = new MemoryStoreFactory();

    private JsonSupport jsonSupport;

    private AuthorizationListener authorizationListener = new SuccessAuthorizationListener();

    private HttpRequestDecoderConfiguration httpRequestDecoderConfiguration = new HttpRequestDecoderConfiguration();

    public Configuration() {
        super();
    }

    public Configuration(BasicConfiguration basicConfiguration) {
        super(basicConfiguration);
    }

    /**
     * Defend from further modifications by cloning
     *
     * @param conf - Configuration object to clone
     */
    Configuration(Configuration conf) {
        super(conf);

        if (conf.getJsonSupport() == null) {
            try {
                getClass().getClassLoader().loadClass("com.fasterxml.jackson.databind.ObjectMapper");
                try {
                    Class<?> jjs = getClass().getClassLoader().loadClass("com.corundumstudio.socketio.protocol.JacksonJsonSupport");
                    JsonSupport js = (JsonSupport) jjs.getConstructor().newInstance();
                    conf.setJsonSupport(js);
                } catch (Exception e) {
                    throw new IllegalArgumentException(e);
                }
            } catch (ClassNotFoundException e) {
                throw new IllegalArgumentException("Can't find jackson lib in classpath", e);
            }
        }

        setJsonSupport(new JsonSupportWrapper(conf.getJsonSupport()));

        setSocketSslConfig(conf.getSocketSslConfig());
        setStoreFactory(conf.getStoreFactory());
        setAuthorizationListener(conf.getAuthorizationListener());
        setExceptionListener(conf.getExceptionListener());
        setSocketConfig(conf.getSocketConfig());

        setHttpRequestDecoderConfiguration(conf.getHttpRequestDecoderConfiguration());
    }

    public JsonSupport getJsonSupport() {
        return jsonSupport;
    }

    /**
     * Allows to setup custom implementation of
     * JSON serialization/deserialization
     *
     * @param jsonSupport - json mapper
     *
     * @see JsonSupport
     */
    public void setJsonSupport(JsonSupport jsonSupport) {
        this.jsonSupport = jsonSupport;
    }


    /**
     * Data store - used to store session data and implements distributed pubsub.
     * Default is {@code MemoryStoreFactory}
     *
     * @param clientStoreFactory - implements StoreFactory
     *
     * @see com.corundumstudio.socketio.store.MemoryStoreFactory
     * @see com.corundumstudio.socketio.store.RedissonStoreFactory
     * @see com.corundumstudio.socketio.store.HazelcastStoreFactory
     */
    public void setStoreFactory(StoreFactory clientStoreFactory) {
        this.storeFactory = clientStoreFactory;
    }

    public StoreFactory getStoreFactory() {
        return storeFactory;
    }

    /**
     * Authorization listener invoked on every handshake.
     * Accepts or denies a client by {@code AuthorizationListener.getAuthorizationResult} method.
     * <b>Accepts</b> all clients by default.
     *
     * @param authorizationListener - authorization listener itself
     *
     * @see com.corundumstudio.socketio.AuthorizationListener
     */
    public void setAuthorizationListener(AuthorizationListener authorizationListener) {
        this.authorizationListener = authorizationListener;
    }

    public AuthorizationListener getAuthorizationListener() {
        return authorizationListener;
    }

    /**
     * Exception listener invoked on any exception in
     * SocketIO listener
     *
     * @param exceptionListener - listener
     *
     * @see com.corundumstudio.socketio.listener.ExceptionListener
     */
    public void setExceptionListener(ExceptionListener exceptionListener) {
        this.exceptionListener = exceptionListener;
    }

    public ExceptionListener getExceptionListener() {
        return exceptionListener;
    }

    public SocketConfig getSocketConfig() {
        return socketConfig;
    }

    /**
     * TCP socket configuration
     *
     * @param socketConfig - config
     */
    public void setSocketConfig(SocketConfig socketConfig) {
        this.socketConfig = socketConfig;
    }

    public HttpRequestDecoderConfiguration getHttpRequestDecoderConfiguration() {
        return httpRequestDecoderConfiguration;
    }

    public void setHttpRequestDecoderConfiguration(HttpRequestDecoderConfiguration httpRequestDecoderConfiguration) {
        this.httpRequestDecoderConfiguration = httpRequestDecoderConfiguration;
    }

    public HttpDecoderConfig getHttpDecoderConfig() {
        return new HttpDecoderConfig()
                .setMaxInitialLineLength(httpRequestDecoderConfiguration.getMaxInitialLineLength())
                .setMaxHeaderSize(httpRequestDecoderConfiguration.getMaxHeaderSize())
                .setMaxChunkSize(httpRequestDecoderConfiguration.getMaxChunkSize());
    }

    public SocketSslConfig getSocketSslConfig() {
        return socketSslConfig;
    }

    public void setSocketSslConfig(SocketSslConfig socketSslConfig) {
        this.socketSslConfig = socketSslConfig;
    }
}
