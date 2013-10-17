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

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.listener.ClientListeners;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.corundumstudio.socketio.namespace.Namespace;
import com.corundumstudio.socketio.namespace.NamespacesHub;

public class SocketIOServer implements ClientListeners {

	private final Logger log = LoggerFactory.getLogger(getClass());

	private final Configuration configCopy;
	private final Configuration configuration;

	private final NamespacesHub namespacesHub;
	private final SocketIONamespace mainNamespace;

	private SocketIOChannelInitializer pipelineFactory = new SocketIOChannelInitializer();

	private EventLoopGroup bossGroup;
	private EventLoopGroup workerGroup;

	public SocketIOServer(Configuration configuration) {
		this.configuration = configuration;
		this.configCopy = new Configuration(configuration);
		namespacesHub = new NamespacesHub(this.configCopy.getJsonSupport());
		mainNamespace = addNamespace(Namespace.DEFAULT_NAME);
	}

	public void setPipelineFactory(SocketIOChannelInitializer pipelineFactory) {
		this.pipelineFactory = pipelineFactory;
	}

	public SocketIOChannelInitializer getPipelineFactory() {
		return this.pipelineFactory;
	}

	/**
	 * Get all clients
	 *
	 * @return clients collection
	 */
	public Collection<SocketIOClient> getAllClients() {
		return pipelineFactory.getAllClients();
	}

	public BroadcastOperations getBroadcastOperations() {
		return getBroadcastOperations(pipelineFactory.getAllClients());
	}

	/**
	 * Get broadcast operations for clients within
	 * room by <code>roomKey</code>
	 *
	 * @param roomKey - any object with correct hashcode & equals implementation
	 * @return
	 */
	public <T> BroadcastOperations getRoomOperations(T roomKey) {
		Iterable<SocketIOClient> clients = namespacesHub.getRoomClients(roomKey);
		return new BroadcastOperations(clients);
	}

	public BroadcastOperations getBroadcastOperations(Iterable<SocketIOClient> clients) {
		return new BroadcastOperations(clients);
	}

	/**
	 * Start server
	 */
	public void start() {
		EventLoopGroup bossGroup = new NioEventLoopGroup();
		EventLoopGroup workerGroup = new NioEventLoopGroup();

		pipelineFactory.start(configCopy, namespacesHub);
		ServerBootstrap b = new ServerBootstrap();
		b.group(bossGroup, workerGroup)
		.option(ChannelOption.TCP_NODELAY, true)
		.option(ChannelOption.SO_KEEPALIVE, true)
		.channel(NioServerSocketChannel.class)
		.childHandler(pipelineFactory);

		InetSocketAddress addr = new InetSocketAddress(configCopy.getPort());
		if (configCopy.getHostname() != null) {
			addr = new InetSocketAddress(configCopy.getHostname(), configCopy.getPort());
		}

		b.bind(addr).syncUninterruptibly();
		log.info("SocketIO server started at port: {}", configCopy.getPort());
	}

	/**
	 * Stop server
	 */
	public void stop() {
		bossGroup.shutdownGracefully();
		workerGroup.shutdownGracefully();
	}

	public SocketIONamespace addNamespace(String name) {
		return namespacesHub.create(name);
	}

	public SocketIONamespace getNamespace(String name) {
		return namespacesHub.get(name);
	}

	public void removeNamespace(String name) {
		namespacesHub.remove(name);
	}

	/**
	 * Allows to get configuration provided
	 * during server creation. Further changes on
	 * this object not affect server.
	 *
	 * @return Configuration object
	 */
	public Configuration getConfiguration() {
		return configuration;
	}

	@Override
	public <T> void addEventListener(String eventName, Class<T> eventClass, DataListener<T> listener) {
		mainNamespace.addEventListener(eventName, eventClass, listener);
	}

	@Override
	public <T> void addJsonObjectListener(Class<T> clazz, DataListener<T> listener) {
		mainNamespace.addJsonObjectListener(clazz, listener);
	}

	@Override
	public void addDisconnectListener(DisconnectListener listener) {
		mainNamespace.addDisconnectListener(listener);
	}

	@Override
	public void addConnectListener(ConnectListener listener) {
		mainNamespace.addConnectListener(listener);
	}

	@Override
	public void addMessageListener(DataListener<String> listener) {
		mainNamespace.addMessageListener(listener);
	}

	@Override
	public void addListeners(Object listeners) {
		mainNamespace.addListeners(listeners);
	}

	@Override
	public void addListeners(Object listeners, Class listenersClass) {
		mainNamespace.addListeners(listeners, listenersClass);
	}


}
