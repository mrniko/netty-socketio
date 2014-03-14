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
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketException;
import java.util.Collection;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.listener.ClientListeners;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.corundumstudio.socketio.listener.ExceptionListener;
import com.corundumstudio.socketio.listener.MultiTypeEventListener;
import com.corundumstudio.socketio.namespace.Namespace;
import com.corundumstudio.socketio.namespace.NamespacesHub;

/**
 * Fully thread-safe.
 *
 */
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
        namespacesHub = new NamespacesHub(configCopy.getJsonSupport(), configCopy.getStoreFactory(), configCopy.getExceptionListener());
        mainNamespace = addNamespace(Namespace.DEFAULT_NAME);
    }

    public void setPipelineFactory(SocketIOChannelInitializer pipelineFactory) {
        this.pipelineFactory = pipelineFactory;
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
        return new BroadcastOperations(pipelineFactory.getAllClients(), configCopy.getStoreFactory());
    }

    /**
     * Get broadcast operations for clients within
     * room by <code>room</code> name
     *
     * @param room
     * @return
     */
    public BroadcastOperations getRoomOperations(String room) {
        Iterable<SocketIOClient> clients = namespacesHub.getRoomClients(room);
        return new BroadcastOperations(clients, configCopy.getStoreFactory());
    }

    /**
     * Start server
     */
    public void start() {
        initGroups();

        pipelineFactory.start(configCopy, namespacesHub);
        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
            .channel(NioServerSocketChannel.class)
            .childHandler(pipelineFactory);
        applyConnectionOptions(b);

        InetSocketAddress addr = new InetSocketAddress(configCopy.getPort());
        if (configCopy.getHostname() != null) {
            addr = new InetSocketAddress(configCopy.getHostname(), configCopy.getPort());
        }

        b.bind(addr).syncUninterruptibly();

        log.info("Session store / pubsub factory used: {}", configCopy.getStoreFactory());
        log.info("SocketIO server started at port: {}", configCopy.getPort());
    }

    protected void applyConnectionOptions(ServerBootstrap bootstrap) {
        SocketConfig config = configCopy.getSocketConfig();
        bootstrap.childOption(ChannelOption.TCP_NODELAY, config.isTcpNoDelay());
        if (config.getTcpSendBufferSize() != -1) {
            bootstrap.childOption(ChannelOption.SO_SNDBUF, config.getTcpSendBufferSize());
        }
        if (config.getTcpReceiveBufferSize() != -1) {
            bootstrap.childOption(ChannelOption.SO_RCVBUF, config.getTcpReceiveBufferSize());
            bootstrap.childOption(ChannelOption.RCVBUF_ALLOCATOR, new FixedRecvByteBufAllocator(config.getTcpReceiveBufferSize()));
        }
        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, config.isTcpKeepAlive());

        bootstrap.option(ChannelOption.SO_LINGER, config.getSoLinger());
        bootstrap.option(ChannelOption.SO_REUSEADDR, config.isReuseAddress());
        bootstrap.option(ChannelOption.SO_BACKLOG, config.getAcceptBackLog());
    }

    protected void initGroups() {
        bossGroup = new NioEventLoopGroup(configCopy.getBossThreads());
        workerGroup = new NioEventLoopGroup(configCopy.getWorkerThreads());
    }

    /**
     * Stop server
     */
    public void stop() {
        bossGroup.shutdownGracefully().syncUninterruptibly();
        workerGroup.shutdownGracefully().syncUninterruptibly();

        pipelineFactory.stop();
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
    public void addMultiTypeEventListener(String eventName, MultiTypeEventListener listener, Class<?>... eventClass) {
        mainNamespace.addMultiTypeEventListener(eventName, listener, eventClass);
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
