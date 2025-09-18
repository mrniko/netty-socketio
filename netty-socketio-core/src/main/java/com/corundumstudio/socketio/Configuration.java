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

import java.io.InputStream;

import javax.net.ssl.KeyManagerFactory;

import com.corundumstudio.socketio.handler.SuccessAuthorizationListener;
import com.corundumstudio.socketio.listener.DefaultExceptionListener;
import com.corundumstudio.socketio.listener.ExceptionListener;
import com.corundumstudio.socketio.protocol.JsonSupport;
import com.corundumstudio.socketio.store.MemoryStoreFactory;
import com.corundumstudio.socketio.store.StoreFactory;

import io.netty.handler.codec.http.HttpDecoderConfig;

public class Configuration extends BasicConfiguration {

    private ExceptionListener exceptionListener = new DefaultExceptionListener();

    private String sslProtocol = "TLSv1";

    private String keyStoreFormat = "JKS";
    private InputStream keyStore;
    private String keyStorePassword;

    private String trustStoreFormat = "JKS";
    private InputStream trustStore;
    private String trustStorePassword;

    private String keyManagerFactoryAlgorithm = KeyManagerFactory.getDefaultAlgorithm();

    private SocketConfig socketConfig = new SocketConfig();

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

        setKeyStorePassword(conf.getKeyStorePassword());
        setKeyStore(conf.getKeyStore());
        setKeyStoreFormat(conf.getKeyStoreFormat());
        setTrustStore(conf.getTrustStore());
        setTrustStoreFormat(conf.getTrustStoreFormat());
        setTrustStorePassword(conf.getTrustStorePassword());
        setKeyManagerFactoryAlgorithm(conf.getKeyManagerFactoryAlgorithm());

        setStoreFactory(conf.getStoreFactory());
        setAuthorizationListener(conf.getAuthorizationListener());
        setExceptionListener(conf.getExceptionListener());
        setSocketConfig(conf.getSocketConfig());

        setSSLProtocol(conf.getSSLProtocol());

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
     * SSL key store password
     *
     * @param keyStorePassword - password of key store
     */
    public void setKeyStorePassword(String keyStorePassword) {
        this.keyStorePassword = keyStorePassword;
    }

    public String getKeyStorePassword() {
        return keyStorePassword;
    }

    /**
     * SSL key store stream, maybe appointed to any source
     *
     * @param keyStore - key store input stream
     */
    public void setKeyStore(InputStream keyStore) {
        this.keyStore = keyStore;
    }

    public InputStream getKeyStore() {
        return keyStore;
    }

    /**
     * Key store format
     *
     * @param keyStoreFormat - key store format
     */
    public void setKeyStoreFormat(String keyStoreFormat) {
        this.keyStoreFormat = keyStoreFormat;
    }

    public String getKeyStoreFormat() {
        return keyStoreFormat;
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


    public String getTrustStoreFormat() {
        return trustStoreFormat;
    }

    public void setTrustStoreFormat(String trustStoreFormat) {
        this.trustStoreFormat = trustStoreFormat;
    }

    public InputStream getTrustStore() {
        return trustStore;
    }

    public void setTrustStore(InputStream trustStore) {
        this.trustStore = trustStore;
    }

    public String getTrustStorePassword() {
        return trustStorePassword;
    }

    public void setTrustStorePassword(String trustStorePassword) {
        this.trustStorePassword = trustStorePassword;
    }

    public String getKeyManagerFactoryAlgorithm() {
        return keyManagerFactoryAlgorithm;
    }

    public void setKeyManagerFactoryAlgorithm(String keyManagerFactoryAlgorithm) {
        this.keyManagerFactoryAlgorithm = keyManagerFactoryAlgorithm;
    }

    /**
     * Set the name of the requested SSL protocol
     *
     * @param sslProtocol - name of protocol
     */
    public void setSSLProtocol(String sslProtocol) {
        this.sslProtocol = sslProtocol;
    }

    public String getSSLProtocol() {
        return sslProtocol;
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
}
