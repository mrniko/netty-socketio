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

import java.net.InetSocketAddress;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.listener.ClientListeners;
import com.corundumstudio.socketio.listener.ConnectListener;
import com.corundumstudio.socketio.listener.DataListener;
import com.corundumstudio.socketio.listener.DisconnectListener;
import com.corundumstudio.socketio.listener.EventInterceptor;
import com.corundumstudio.socketio.listener.MultiTypeEventListener;
import com.corundumstudio.socketio.listener.PingListener;
import com.corundumstudio.socketio.listener.PongListener;
import com.corundumstudio.socketio.namespace.Namespace;
import com.corundumstudio.socketio.namespace.NamespacesHub;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelOption;
import io.netty.channel.DefaultEventLoop;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.FixedRecvByteBufAllocator;
import io.netty.channel.ServerChannel;
import io.netty.channel.WriteBufferWaterMark;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.util.concurrent.Future;
import io.netty.util.concurrent.FutureListener;
import io.netty.util.concurrent.SucceededFuture;

/**
 * Fully thread-safe.
 *
 */
public class SocketIOServer implements ClientListeners {

    private static final Logger log = LoggerFactory.getLogger(SocketIOServer.class);

    private final AtomicReference<ServerStatus> serverStatus = new AtomicReference<ServerStatus>(ServerStatus.INIT);

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
        namespacesHub = new NamespacesHub(configCopy);
        mainNamespace = addNamespace(Namespace.DEFAULT_NAME);
    }

    public void setPipelineFactory(SocketIOChannelInitializer pipelineFactory) {
        this.pipelineFactory = pipelineFactory;
    }

    /**
     * Get all clients connected to default namespace
     *
     * @return clients collection
     */
    public Collection<SocketIOClient> getAllClients() {
        return namespacesHub.get(Namespace.DEFAULT_NAME).getAllClients();
    }

    /**
     * Get client by uuid from default namespace
     *
     * @param uuid - id of client
     * @return client
     */
    public SocketIOClient getClient(UUID uuid) {
        return namespacesHub.get(Namespace.DEFAULT_NAME).getClient(uuid);
    }

    /**
     * Get all namespaces
     *
     * @return namespaces collection
     */
    public Collection<SocketIONamespace> getAllNamespaces() {
        return namespacesHub.getAllNamespaces();
    }

    public BroadcastOperations getBroadcastOperations() {
        Collection<SocketIONamespace> namespaces = namespacesHub.getAllNamespaces();
        List<BroadcastOperations> list = new ArrayList<BroadcastOperations>();
        BroadcastOperations broadcast = null;
        if (namespaces != null && namespaces.size() > 0) {
            for (SocketIONamespace n : namespaces) {
                broadcast = n.getBroadcastOperations();
                list.add(broadcast);
            }
        }
        return new MultiRoomBroadcastOperations(list);
    }

    /**
     * Get broadcast operations for clients within
     * rooms by <code>rooms'</code> names
     *
     * @param rooms rooms' names
     * @return broadcast operations
     */
    public BroadcastOperations getRoomOperations(String... rooms) {
        Collection<SocketIONamespace> namespaces = namespacesHub.getAllNamespaces();
        List<BroadcastOperations> list = new ArrayList<BroadcastOperations>();
        BroadcastOperations broadcast = null;
        if (namespaces != null && namespaces.size() > 0) {
            for (SocketIONamespace n : namespaces) {
                for (String room : rooms) {
                    broadcast = n.getRoomOperations(room);
                    list.add(broadcast);
                }
            }
        }
        return new MultiRoomBroadcastOperations(list);
    }

    /**
     * Start server
     */
    public void start() {
        startAsync().syncUninterruptibly();
    }

    /**
     * Returns true if server is started
     */
    public boolean isStarted() {
        return serverStatus.get() == ServerStatus.STARTED;
    }

    /**
     * Start server asynchronously
     *
     * @return void
     */
    public Future<Void> startAsync() {
        if (!serverStatus.compareAndSet(ServerStatus.INIT, ServerStatus.STARTING)) {
            log.warn("Invalid server state: {}, should be: {}, ignoring start request", serverStatus.get(), ServerStatus.INIT);
            return new SucceededFuture<Void>(new DefaultEventLoop(), null);
        }
        log.info("Session store / pubsub factory used: {}", configCopy.getStoreFactory());
        initGroups();

        pipelineFactory.start(configCopy, namespacesHub);

        Class<? extends ServerChannel> channelClass = NioServerSocketChannel.class;
        if (configCopy.isUseLinuxNativeEpoll()) {
            channelClass = EpollServerSocketChannel.class;
        }

        ServerBootstrap b = new ServerBootstrap();
        b.group(bossGroup, workerGroup)
                .channel(channelClass)
                .childHandler(pipelineFactory);
        applyConnectionOptions(b);

        InetSocketAddress addr = new InetSocketAddress(configCopy.getPort());
        if (configCopy.getHostname() != null) {
            addr = new InetSocketAddress(configCopy.getHostname(), configCopy.getPort());
        }

        return b.bind(addr).addListener(new FutureListener<Void>() {
            @Override
            public void operationComplete(Future<Void> future) throws Exception {
                if (future.isSuccess()) {
                    serverStatus.set(ServerStatus.STARTED);
                    log.info("SocketIO server started at port: {}", configCopy.getPort());
                } else {
                    serverStatus.set(ServerStatus.INIT);
                    log.error("SocketIO server start failed at port: {}!", configCopy.getPort());
                }
            }
        });
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
        //default value @see WriteBufferWaterMark.DEFAULT
        if (config.getWriteBufferWaterMarkLow() != -1 && config.getWriteBufferWaterMarkHigh() != -1) {
            bootstrap.childOption(ChannelOption.WRITE_BUFFER_WATER_MARK, new WriteBufferWaterMark(
                    config.getWriteBufferWaterMarkLow(), config.getWriteBufferWaterMarkHigh()
            ));
        }

        bootstrap.childOption(ChannelOption.SO_KEEPALIVE, config.isTcpKeepAlive());
        bootstrap.childOption(ChannelOption.SO_LINGER, config.getSoLinger());

        bootstrap.option(ChannelOption.SO_REUSEADDR, config.isReuseAddress());
        bootstrap.option(ChannelOption.SO_BACKLOG, config.getAcceptBackLog());
    }

    protected void initGroups() {
        if (configCopy.isUseLinuxNativeEpoll()) {
            bossGroup = new EpollEventLoopGroup(configCopy.getBossThreads());
            workerGroup = new EpollEventLoopGroup(configCopy.getWorkerThreads());
        } else {
            bossGroup = new NioEventLoopGroup(configCopy.getBossThreads());
            workerGroup = new NioEventLoopGroup(configCopy.getWorkerThreads());
        }
    }

    /**
     * Stop server
     */
    public void stop() {
        if (!serverStatus.compareAndSet(ServerStatus.STARTED, ServerStatus.STOPPING)) {
            log.warn("Invalid server state: {}, should be: {}, ignoring stop request", serverStatus.get(), ServerStatus.STARTED);
            return;
        }
        try {
            log.info("Stopping SocketIO server...");
            bossGroup.shutdownGracefully().syncUninterruptibly();
            workerGroup.shutdownGracefully().syncUninterruptibly();
            pipelineFactory.stop();
            log.info("SocketIO server stopped");
        } finally {
            serverStatus.set(ServerStatus.INIT);
        }
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
    public void addEventInterceptor(EventInterceptor eventInterceptor) {
        mainNamespace.addEventInterceptor(eventInterceptor);

    }


    @Override
    public void removeAllListeners(String eventName) {
        mainNamespace.removeAllListeners(eventName);
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
    public void addPingListener(PingListener listener) {
        mainNamespace.addPingListener(listener);
    }

    @Override
    public void addPongListener(PongListener listener) {
        mainNamespace.addPongListener(listener);
    }

    @Override
    public void addListeners(Object listeners) {
        mainNamespace.addListeners(listeners);
    }

    @Override
    public <L> void addListeners(Iterable<L> listeners) {
        mainNamespace.addListeners(listeners);
    }

    @Override
    public void addListeners(Object listeners, Class<?> listenersClass) {
        mainNamespace.addListeners(listeners, listenersClass);
    }


}
