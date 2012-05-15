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

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.codehaus.jackson.map.ObjectMapper;

public class Configuration {

	private String context = "/socket.io";

    private Executor bossExecutor = Executors.newCachedThreadPool();
    private Executor workerExecutor = Executors.newCachedThreadPool();

    private int heartbeatThreadPoolSize = Runtime.getRuntime().availableProcessors() * 2;
    private int heartbeatTimeout = 60;
    private int heartbeatInterval = 25;
    private int closeTimeout = 60;

    private String hostname;
    private int port;

    private ObjectMapper objectMapper = new ObjectMapper();
    private SocketIOListener listener;

    public Configuration() {
    }

    /**
     * Defend from further modifications by cloning
     *
     * @param configuration - Configuration object to clone
     */
    public Configuration(Configuration conf) {
        setBossExecutor(conf.getBossExecutor());
        setCloseTimeout(conf.getCloseTimeout());
        setHeartbeatInterval(conf.getHeartbeatInterval());
        setHeartbeatThreadPoolSize(conf.getHeartbeatThreadPoolSize());
        setHeartbeatTimeout(conf.getHeartbeatTimeout());
        setHostname(conf.getHostname());
        setListener(conf.getListener());
        setObjectMapper(conf.getObjectMapper());
        setPort(conf.getPort());
        setWorkerExecutor(conf.getWorkerExecutor());
    }

    public ObjectMapper getObjectMapper() {
        return objectMapper;
    }
    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public SocketIOListener getListener() {
        return listener;
    }
    public void setListener(SocketIOListener listener) {
        this.listener = listener;
    }

    public String getHostname() {
        return hostname;
    }
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

}
