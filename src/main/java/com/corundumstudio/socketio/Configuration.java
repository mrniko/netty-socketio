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
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.jboss.netty.handler.codec.frame.TooLongFrameException;

import com.corundumstudio.socketio.parser.JacksonJsonSupport;
import com.corundumstudio.socketio.parser.JsonSupport;
import com.corundumstudio.socketio.parser.JsonSupportWrapper;

public class Configuration {

    private String jsonTypeFieldName = "@class";
    private String context = "/socket.io";

    private String transports = join(new Transport[] {Transport.WEBSOCKET, Transport.FLASHSOCKET, Transport.XHRPOLLING});

    private Executor bossExecutor = Executors.newCachedThreadPool();
    private Executor workerExecutor = Executors.newCachedThreadPool();

    private boolean allowCustomRequests = false;

    private int pollingDuration = 20;

    private int heartbeatThreadPoolSize = Runtime.getRuntime().availableProcessors() * 2;
    private int heartbeatTimeout = 60;
    private int heartbeatInterval = 25;
    private int closeTimeout = 60;

    private int maxHttpContentLength = 64 * 1024;

    private String hostname;
    private int port = -1;

    private InputStream keyStore;
    private String keyStorePassword;

    private JsonSupport jsonSupport = new JacksonJsonSupport(this);

    public Configuration() {
    }

    /**
     * Defend from further modifications by cloning
     *
     * @param configuration - Configuration object to clone
     */
    Configuration(Configuration conf) {
        setBossExecutor(conf.getBossExecutor());
        setCloseTimeout(conf.getCloseTimeout());
        setHeartbeatInterval(conf.getHeartbeatInterval());
        setHeartbeatThreadPoolSize(conf.getHeartbeatThreadPoolSize());
        setHeartbeatTimeout(conf.getHeartbeatTimeout());
        setHostname(conf.getHostname());
        setJsonSupport(new JsonSupportWrapper(conf.getJsonSupport()));
        setPort(conf.getPort());
        setWorkerExecutor(conf.getWorkerExecutor());
        setContext(conf.getContext());
        setAllowCustomRequests(conf.isAllowCustomRequests());
        setPollingDuration(conf.getPollingDuration());
        setJsonTypeFieldName(conf.getJsonTypeFieldName());
        setKeyStorePassword(conf.getKeyStorePassword());
        setKeyStore(conf.getKeyStore());
        setTransports(conf.getTransports());
        setMaxHttpContentLength(conf.getMaxHttpContentLength());
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

    public Executor getBossExecutor() {
        return bossExecutor;
    }
    public void setBossExecutor(Executor bossExecutor) {
        this.bossExecutor = bossExecutor;
    }

    public Executor getWorkerExecutor() {
        return workerExecutor;
    }
    public void setWorkerExecutor(Executor workerExecutor) {
        this.workerExecutor = workerExecutor;
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
     *
     * @param allowCustomRequests - true to allow
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

}
