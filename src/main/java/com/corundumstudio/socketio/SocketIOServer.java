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

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketIOServer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private int heartbeatThreadPoolSize = 4;
    private int heartbeatTimeout = 60;
    private int heartbeatInterval = 25;
    private int heartbeatIntervalDiff = 5;
    private int bossThreadPoolSize = 8;
    private int workerThreadPoolSize = 16;

    private ServerBootstrap bootstrap;
    private Channel mainChannel;

    private ObjectMapper objectMapper = new ObjectMapper();
    private SocketIOListener listener;
    private SocketIORouter socketIORouter;
    private String hostname;
    private int port;

    public SocketIOServer() {
        objectMapper.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
    }

    public void start() {
        Executor bossExecutor = Executors.newFixedThreadPool(bossThreadPoolSize);
        Executor workerExecutor = Executors.newFixedThreadPool(workerThreadPoolSize);
        ChannelFactory factory = new NioServerSocketChannelFactory(bossExecutor, workerExecutor);
        bootstrap = new ServerBootstrap(factory);

        socketIORouter = new SocketIORouter(listener, objectMapper);
        socketIORouter.setHeartbeatInterval(heartbeatInterval);
        socketIORouter.setHeartbeatTimeout(heartbeatTimeout);
        socketIORouter.setHeartbeatThreadPoolSize(heartbeatThreadPoolSize);
        socketIORouter.setHeartbeatIntervalDiff(heartbeatIntervalDiff);
        socketIORouter.start();

        SocketIOUpstreamHandler upstreamHandler = new SocketIOUpstreamHandler(socketIORouter);
        SocketIOPipelineFactory pipelineFactory = new SocketIOPipelineFactory(upstreamHandler);
        bootstrap.setPipelineFactory(pipelineFactory);
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);
        mainChannel = bootstrap.bind(new InetSocketAddress(hostname, port));
        log.info("SocketIO server started at port: {}", port);
    }

    public void setWorkerThreadPoolSize(int workerThreadPoolSize) {
        this.workerThreadPoolSize = workerThreadPoolSize;
    }

    public void setBossThreadPoolSize(int bossThreadPoolSize) {
        this.bossThreadPoolSize = bossThreadPoolSize;
    }

    /**
     * Heartbeat interval difference, because server should send response a little bit earlier
     *
     * @param value
     *            - time in seconds
     */
    public void setHeartbeatIntervalDiff(int heartbeatIntervalDiff) {
        this.heartbeatIntervalDiff = heartbeatIntervalDiff;
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

    /**
     * Heartbeat timeout
     *
     * @param value
     *            - time in seconds
     */
    public void setHeartbeatTimeout(int heartbeatTimeoutSecs) {
        this.heartbeatTimeout = heartbeatTimeoutSecs;
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

    public void stop() {
        socketIORouter.stop();
        bootstrap.releaseExternalResources();
    }

    public void setHostname(String hostname) {
        this.hostname = hostname;
    }

    public void setPort(int port) {
        this.port = port;
    }

    public void setListener(SocketIOListener socketIOHandler) {
        this.listener = socketIOHandler;
    }

    public void setObjectMapper(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

}
