/**
 * Copyright 2012 Nikita Koksharov
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
import java.util.Arrays;
import java.util.List;

import com.corundumstudio.socketio.handler.SuccessAuthorizationListener;
import com.corundumstudio.socketio.listener.DefaultExceptionListener;
import com.corundumstudio.socketio.listener.ExceptionListener;
import com.corundumstudio.socketio.protocol.JacksonJsonSupport;
import com.corundumstudio.socketio.protocol.JsonSupport;
import com.corundumstudio.socketio.store.MemoryStoreFactory;
import com.corundumstudio.socketio.store.StoreFactory;

public class Configuration {

    private ExceptionListener exceptionListener = new DefaultExceptionListener();

    private String context = "/socket.io";

    private List<Transport> transports = Arrays.asList(Transport.WEBSOCKET, Transport.POLLING);

    private int bossThreads = 0; // 0 = current_processors_amount * 2
    private int workerThreads = 0; // 0 = current_processors_amount * 2
    private boolean useLinuxNativeEpoll;

    private boolean allowCustomRequests = false;

    private int upgradeTimeout = 10000;
    private int pingTimeout = 60000;
    private int pingInterval = 25000;

    private int maxHttpContentLength = 64 * 1024;
    private int maxFramePayloadLength = 64 * 1024;

    private String packagePrefix;
    private String hostname;
    private int port = -1;

    private String keyStoreFormat = "JKS";
    private InputStream keyStore;
    private String keyStorePassword;

    private String trustStoreFormat = "JKS";
    private InputStream trustStore;
    private String trustStorePassword;

    private boolean preferDirectBuffer = true;

    private SocketConfig socketConfig = new SocketConfig();

    private StoreFactory storeFactory = new MemoryStoreFactory();

    private JsonSupport jsonSupport = new JacksonJsonSupport();

    private AuthorizationListener authorizationListener = new SuccessAuthorizationListener();

    private AckMode ackMode = AckMode.AUTO_SUCCESS_ONLY;

    private boolean addVersionHeader = true;

    private String origin;

    public Configuration() {
    }

    /**
     * Defend from further modifications by cloning
     *
     * @param configuration - Configuration object to clone
     */
    Configuration(Configuration conf) {
        setBossThreads(conf.getBossThreads());
        setWorkerThreads(conf.getWorkerThreads());
        setUseLinuxNativeEpoll(conf.isUseLinuxNativeEpoll());

        setPingInterval(conf.getPingInterval());
        setPingTimeout(conf.getPingTimeout());

        setHostname(conf.getHostname());
        setPort(conf.getPort());

        setJsonSupport(new JsonSupportWrapper(conf.getJsonSupport()));
        setContext(conf.getContext());
        setAllowCustomRequests(conf.isAllowCustomRequests());

        setKeyStorePassword(conf.getKeyStorePassword());
        setKeyStore(conf.getKeyStore());
        setKeyStoreFormat(conf.getKeyStoreFormat());
        setTrustStore(conf.getTrustStore());
        setTrustStoreFormat(conf.getTrustStoreFormat());
        setTrustStorePassword(conf.getTrustStorePassword());

        setTransports(conf.getTransports().toArray(new Transport[conf.getTransports().size()]));
        setMaxHttpContentLength(conf.getMaxHttpContentLength());
        setPackagePrefix(conf.getPackagePrefix());

        setPreferDirectBuffer(conf.isPreferDirectBuffer());
        setStoreFactory(conf.getStoreFactory());
        setAuthorizationListener(conf.getAuthorizationListener());
        setExceptionListener(conf.getExceptionListener());
        setSocketConfig(conf.getSocketConfig());
        setAckMode(conf.getAckMode());
        setMaxFramePayloadLength(conf.getMaxFramePayloadLength());
        setUpgradeTimeout(conf.getUpgradeTimeout());

        setAddVersionHeader(conf.isAddVersionHeader());
        setOrigin(conf.getOrigin());
    }

    public JsonSupport getJsonSupport() {
        return jsonSupport;
    }

    /**
     * Allows to setup custom implementation of
     * JSON serialization/deserialization
     *
     * @param jsonSupport
     *
     * @see JsonSupport
     */
    public void setJsonSupport(JsonSupport jsonSupport) {
        this.jsonSupport = jsonSupport;
    }

    public String getHostname() {
        return hostname;
    }

    /**
     * Optional parameter. If not set then bind address
     * will be 0.0.0.0 or ::0
     *
     * @param hostname
     */
    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public int getPort() {
        return port;
    }
    public void setPort(int port) {
        this.port = port;
    }

    public int getBossThreads() {
        return bossThreads;
    }
    public void setBossThreads(int bossThreads) {
        this.bossThreads = bossThreads;
    }

    public int getWorkerThreads() {
        return workerThreads;
    }
    public void setWorkerThreads(int workerThreads) {
        this.workerThreads = workerThreads;
    }

    /**
     * Ping interval
     *
     * @param value
     *            - time in seconds
     */
    public void setPingInterval(int heartbeatIntervalSecs) {
        this.pingInterval = heartbeatIntervalSecs;
    }
    public int getPingInterval() {
        return pingInterval;
    }

    /**
     * Ping timeout
     * Use <code>0</code> to disable it
     *
     * @param value
     *            - time in seconds
     */
    public void setPingTimeout(int heartbeatTimeoutSecs) {
        this.pingTimeout = heartbeatTimeoutSecs;
    }
    public int getPingTimeout() {
        return pingTimeout;
    }
    public boolean isHeartbeatsEnabled() {
        return pingTimeout > 0;
    }

    public String getContext() {
        return context;
    }
    public void setContext(String context) {
        this.context = context;
    }

    public boolean isAllowCustomRequests() {
        return allowCustomRequests;
    }

    /**
     * Allow to service custom requests differs from socket.io protocol.
     * In this case it's necessary to add own handler which handle them
     * to avoid hang connections.
     * Default is {@code false}
     *
     * @param allowCustomRequests - {@code true} to allow
     */
    public void setAllowCustomRequests(boolean allowCustomRequests) {
        this.allowCustomRequests = allowCustomRequests;
    }

    /**
     * SSL key store password
     *
     * @param keyStorePassword
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
     * @param keyStore
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
     * @param keyStoreFormat
     */
    public void setKeyStoreFormat(String keyStoreFormat) {
        this.keyStoreFormat = keyStoreFormat;
    }
    public String getKeyStoreFormat() {
        return keyStoreFormat;
    }

    /**
     * Set maximum http content length limit
     *
     * @param maxContentLength
     *        the maximum length of the aggregated http content.
     */
    public void setMaxHttpContentLength(int value) {
        this.maxHttpContentLength = value;
    }
    public int getMaxHttpContentLength() {
        return maxHttpContentLength;
    }

    /**
     * Transports supported by server
     *
     * @param transports - list of transports
     */
    public void setTransports(Transport ... transports) {
        if (transports.length == 0) {
            throw new IllegalArgumentException("Transports list can't be empty");
        }
        this.transports = Arrays.asList(transports);
    }
    public List<Transport> getTransports() {
        return transports;
    }

    /**
     * Package prefix for sending json-object from client
     * without full class name.
     *
     * With defined package prefix socket.io client
     * just need to define '@class: 'SomeType'' in json object
     * instead of '@class: 'com.full.package.name.SomeType''
     *
     * @param packagePrefix - prefix string
     *
     */
    public void setPackagePrefix(String packagePrefix) {
        this.packagePrefix = packagePrefix;
    }
    public String getPackagePrefix() {
        return packagePrefix;
    }

    /**
     * Buffer allocation method used during packet encoding.
     * Default is {@code true}
     *
     * @param preferDirectBuffer    {@code true} if a direct buffer should be tried to be used as target for
     *                              the encoded messages. If {@code false} is used it will allocate a heap
     *                              buffer, which is backed by an byte array.
     */
    public void setPreferDirectBuffer(boolean preferDirectBuffer) {
        this.preferDirectBuffer = preferDirectBuffer;
    }
    public boolean isPreferDirectBuffer() {
        return preferDirectBuffer;
    }

    /**
     * Data store - used to store session data and implements distributed pubsub.
     * Default is {@code MemoryStoreFactory}
     *
     * @param storeFactory - implements StoreFactory
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
     * Accepts or denies a client by {@code AuthorizationListener.isAuthorized} method.
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
     * @param exceptionListener
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
     * @param socketConfig
     */
    public void setSocketConfig(SocketConfig socketConfig) {
        this.socketConfig = socketConfig;
    }

    /**
     * Auto ack-response mode
     * Default is {@code AckMode.AUTO_SUCCESS_ONLY}
     *
     * @see AckMode
     *
     * @param ackMode
     */
    public void setAckMode(AckMode ackMode) {
        this.ackMode = ackMode;
    }
    public AckMode getAckMode() {
        return ackMode;
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

    /**
     * Set maximum websocket frame content length limit
     *
     * @param maxContentLength
     */
    public void setMaxFramePayloadLength(int maxFramePayloadLength) {
        this.maxFramePayloadLength = maxFramePayloadLength;
    }
    public int getMaxFramePayloadLength() {
        return maxFramePayloadLength;
    }

    /**
     * Transport upgrade timeout in milliseconds
     *
     * @param upgradeTimeout
     */
    public void setUpgradeTimeout(int upgradeTimeout) {
        this.upgradeTimeout = upgradeTimeout;
    }
    public int getUpgradeTimeout() {
        return upgradeTimeout;
    }

    /**
     * Adds <b>Server</b> header with lib version to http response.
     * Default is <code>true</code>
     *
     * @param addVersionHeader
     */
    public void setAddVersionHeader(boolean addVersionHeader) {
        this.addVersionHeader = addVersionHeader;
    }
    public boolean isAddVersionHeader() {
        return addVersionHeader;
    }

    /**
     * Set <b>Access-Control-Allow-Origin</b> header value for http each
     * response.
     * Default is <code>null</code>
     *
     * If value is <code>null</code> then request <b>ORIGIN</b> header value used.
     *
     * @param origin
     */
    public void setOrigin(String origin) {
        this.origin = origin;
    }
    public String getOrigin() {
        return origin;
    }

    public boolean isUseLinuxNativeEpoll() {
        return useLinuxNativeEpoll;
    }
    public void setUseLinuxNativeEpoll(boolean useLinuxNativeEpoll) {
        this.useLinuxNativeEpoll = useLinuxNativeEpoll;
    }

}
