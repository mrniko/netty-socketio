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

import io.netty.handler.codec.TooLongFrameException;

import java.io.InputStream;

import com.corundumstudio.socketio.parser.JacksonJsonSupport;
import com.corundumstudio.socketio.parser.JsonSupport;

public class Configuration {

    private String jsonTypeFieldName = "@class";
    private String context = "/socket.io";

    private String transports = join(new Transport[] {Transport.WEBSOCKET, Transport.FLASHSOCKET, Transport.XHRPOLLING});

    private int bossThreads = 0; // 0 = current_processors_amount * 2
    private int workerThreads = 0; // 0 = current_processors_amount * 2

    private boolean allowCustomRequests = false;

    private int pollingDuration = 20;

    private int heartbeatThreadPoolSize = Runtime.getRuntime().availableProcessors() * 2;
    private int heartbeatTimeout = 60;
    private int heartbeatInterval = 25;
    private int closeTimeout = 60;

    private int maxHttpContentLength = 64 * 1024;

    private String packagePrefix;
    private String hostname;
    private int port = -1;

    private InputStream keyStore;
    private String keyStorePassword;

    private boolean preferDirectBuffer = true;

    private JsonSupport jsonSupport = new JacksonJsonSupport(this);

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

        setCloseTimeout(conf.getCloseTimeout());

        setHeartbeatInterval(conf.getHeartbeatInterval());
        setHeartbeatThreadPoolSize(conf.getHeartbeatThreadPoolSize());
        setHeartbeatTimeout(conf.getHeartbeatTimeout());

        setHostname(conf.getHostname());
        setPort(conf.getPort());

        setJsonSupport(new JsonSupportWrapper(conf.getJsonSupport()));
        setJsonTypeFieldName(conf.getJsonTypeFieldName());
        setContext(conf.getContext());
        setAllowCustomRequests(conf.isAllowCustomRequests());
        setPollingDuration(conf.getPollingDuration());

        setKeyStorePassword(conf.getKeyStorePassword());
        setKeyStore(conf.getKeyStore());

        setTransports(conf.getTransports());
        setMaxHttpContentLength(conf.getMaxHttpContentLength());
        setPackagePrefix(conf.getPackagePrefix());

        setPreferDirectBuffer(conf.isPreferDirectBuffer());
    }

    private String join(Transport[] transports) {
        StringBuilder result = new StringBuilder();
        for (Transport transport : transports) {
            result.append(transport.getValue());
            result.append(",");
        }
        result.setLength(result.length()-1);
        return result.toString();
    }

    public String getJsonTypeFieldName() {
        return jsonTypeFieldName;
    }
    public void setJsonTypeFieldName(String jsonTypeFieldName) {
        this.jsonTypeFieldName = jsonTypeFieldName;
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
     * Heartbeat interval
     *
     * @param value
     *            - time in seconds
     */
    public void setHeartbeatInterval(int heartbeatIntervalSecs) {
        this.heartbeatInterval = heartbeatIntervalSecs;
    }
    public int getHeartbeatInterval() {
        return heartbeatInterval;
    }

    /**
     * Heartbeat timeout
     * Use <code>0</code> to disable it
     *
     * @param value
     *            - time in seconds
     */
    public void setHeartbeatTimeout(int heartbeatTimeoutSecs) {
        this.heartbeatTimeout = heartbeatTimeoutSecs;
    }
    public int getHeartbeatTimeout() {
        return heartbeatTimeout;
    }
    public boolean isHeartbeatsEnabled() {
        return heartbeatTimeout > 0;
    }

    /**
     * Heartbeat thread pool size
     *
     * @param value
     *            - threads amount
     */
    public void setHeartbeatThreadPoolSize(int heartbeatThreadPoolSize) {
        this.heartbeatThreadPoolSize = heartbeatThreadPoolSize;
    }
    public int getHeartbeatThreadPoolSize() {
        return heartbeatThreadPoolSize;
    }

    /**
     * Channel close timeout due inactivity
     *
     * @param closeTimeout - time in seconds
     */
    public void setCloseTimeout(int closeTimeout) {
        this.closeTimeout = closeTimeout;
    }
    public int getCloseTimeout() {
        return closeTimeout;
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

    public int getPollingDuration() {
        return pollingDuration;
    }

    /**
     * Polling interval for XHR transport
     *
     * @param pollingDuration - time in seconds
     */
    public void setPollingDuration(int pollingDuration) {
        this.pollingDuration = pollingDuration;
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
     * Set maximum http content length limit
     *
     * @param maxContentLength
     *        the maximum length of the aggregated http content.
     *        If the length of the aggregated content exceeds this value,
     *        a {@link TooLongFrameException} will be raised.
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
        this.transports = join(transports);
    }
    // used in cloning
    private void setTransports(String transports) {
        this.transports = transports;
    }
    public String getTransports() {
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

}
