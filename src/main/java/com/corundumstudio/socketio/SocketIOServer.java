/**
 * Copyright (c) 2012 Nikita Koksharov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */
package com.corundumstudio.socketio;

import java.net.InetSocketAddress;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;

import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.netty.bootstrap.ServerBootstrap;
import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFactory;
import org.jboss.netty.channel.socket.nio.NioServerSocketChannelFactory;
import org.jboss.netty.logging.InternalLoggerFactory;
import org.jboss.netty.logging.Slf4JLoggerFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class SocketIOServer {

	private final Logger log = LoggerFactory.getLogger(getClass());
	
	private int heartbeatThreadPoolSize = 4;
	private int heartbeatTimeout = 15;
	private int heartbeatInterval = 20;
	private int bossThreadPoolSize = 8;
	private int workerThreadPoolSize = 16;
	
	private ServerBootstrap bootstrap;
	private Channel mainChannel;

	private ObjectMapper objectMapper = new ObjectMapper();
	private SocketIOListener listener;
	private SocketIORouter socketIORouter;
	private String hostname;
	private int port;

	public void start() {
		InternalLoggerFactory.setDefaultFactory(new Slf4JLoggerFactory());

		Executor bossExecutor = Executors.newFixedThreadPool(bossThreadPoolSize);
		Executor workerExecutor = Executors.newFixedThreadPool(workerThreadPoolSize);
		ChannelFactory factory = new NioServerSocketChannelFactory(bossExecutor, workerExecutor);
		bootstrap = new ServerBootstrap(factory);
		
		socketIORouter = new SocketIORouter(listener, objectMapper);
		socketIORouter.setHeartbeatInterval(heartbeatInterval);
		socketIORouter.setHeartbeatTimeout(heartbeatTimeout);
		socketIORouter.setHeartbeatThreadPoolSize(heartbeatThreadPoolSize);
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
	 * Heartbeat interval
	 * 
	 * @param value - time in seconds
	 */
	public void setHeartbeatInterval(int heartbeatIntervalSecs) {
		this.heartbeatInterval = heartbeatIntervalSecs;
	}

	/**
	 * Heartbeat timeout
	 * 
	 * @param value - time in seconds
	 */
	public void setHeartbeatTimeout(int heartbeatTimeoutSecs) {
		this.heartbeatTimeout = heartbeatTimeoutSecs;
	}

	/**
	 * Heartbeat thread pool size
	 * 
	 * @param value - threads amount
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
