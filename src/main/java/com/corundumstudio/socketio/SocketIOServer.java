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

import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketIOServer {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private ServerBootstrap bootstrap;

    private SocketIORouter socketIORouter;

    private Configuration config;

    public SocketIOServer(Configuration configuration) {
        this.config = new Configuration(configuration);
        this.config.getObjectMapper().setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
    }

    public void start() {
        ChannelFactory factory = new NioServerSocketChannelFactory(config.getBossExecutor(), config.getWorkerExecutor());
        bootstrap = new ServerBootstrap(factory);

        socketIORouter = new SocketIORouter(config);
        socketIORouter.start();

        SocketIOUpstreamHandler upstreamHandler = new SocketIOUpstreamHandler(socketIORouter);
        SocketIOPipelineFactory pipelineFactory = new SocketIOPipelineFactory(upstreamHandler);
        bootstrap.setPipelineFactory(pipelineFactory);
        bootstrap.setOption("child.tcpNoDelay", true);
        bootstrap.setOption("child.keepAlive", true);
        bootstrap.bind(new InetSocketAddress(config.getHostname(), config.getPort()));

        log.info("SocketIO server started at port: {}", config.getPort());
    }

    public void stop() {
        socketIORouter.stop();
        bootstrap.releaseExternalResources();
    }

}
