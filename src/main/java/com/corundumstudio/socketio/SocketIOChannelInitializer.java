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

import io.netty.channel.Channel;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelPipeline;
import io.netty.handler.codec.http.HttpObjectAggregator;
import io.netty.handler.codec.http.HttpRequestDecoder;
import io.netty.handler.codec.http.HttpResponseEncoder;
import io.netty.handler.ssl.SslHandler;

import java.io.InputStream;
import java.security.KeyStore;
import java.security.Security;
import java.util.Collection;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.ack.AckManager;
import com.corundumstudio.socketio.handler.AuthorizeHandler;
import com.corundumstudio.socketio.handler.EncoderHandler;
import com.corundumstudio.socketio.handler.HeartbeatHandler;
import com.corundumstudio.socketio.handler.PacketHandler;
import com.corundumstudio.socketio.handler.PacketListener;
import com.corundumstudio.socketio.handler.ResourceHandler;
import com.corundumstudio.socketio.handler.WrongUrlHandler;
import com.corundumstudio.socketio.misc.CompositeIterable;
import com.corundumstudio.socketio.misc.IterableCollection;
import com.corundumstudio.socketio.namespace.NamespacesHub;
import com.corundumstudio.socketio.parser.Decoder;
import com.corundumstudio.socketio.parser.Encoder;
import com.corundumstudio.socketio.parser.JsonSupport;
import com.corundumstudio.socketio.scheduler.CancelableScheduler;
import com.corundumstudio.socketio.store.StoreFactory;
import com.corundumstudio.socketio.transport.FlashPolicyHandler;
import com.corundumstudio.socketio.transport.FlashSocketTransport;
import com.corundumstudio.socketio.transport.MainBaseClient;
import com.corundumstudio.socketio.transport.WebSocketTransport;
import com.corundumstudio.socketio.transport.XHRPollingTransport;

public class SocketIOChannelInitializer extends ChannelInitializer<Channel> implements DisconnectableHub {

    public static final String SOCKETIO_ENCODER = "socketioEncoder";
    public static final String WEB_SOCKET_TRANSPORT = "webSocketTransport";
    public static final String FLASH_SOCKET_TRANSPORT = "flashSocketTransport";
    public static final String XHR_POLLING_TRANSPORT = "xhrPollingTransport";
    public static final String AUTHORIZE_HANDLER = "authorizeHandler";
    public static final String PACKET_HANDLER = "packetHandler";
    public static final String HTTP_ENCODER = "encoder";
    public static final String HTTP_AGGREGATOR = "aggregator";
    public static final String HTTP_REQUEST_DECODER = "decoder";
    public static final String SSL_HANDLER = "ssl";
    public static final String FLASH_POLICY_HANDLER = "flashPolicyHandler";
    public static final String RESOURCE_HANDLER = "resourceHandler";
    public static final String WRONG_URL_HANDLER = "wrongUrlBlocker";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final int protocol = 1;

    private AckManager ackManager;

    private AuthorizeHandler authorizeHandler;
    private XHRPollingTransport xhrPollingTransport;
    private WebSocketTransport webSocketTransport;
    private FlashSocketTransport flashSocketTransport;
    private final FlashPolicyHandler flashPolicyHandler = new FlashPolicyHandler();
    private ResourceHandler resourceHandler;
    private EncoderHandler encoderHandler;
    private WrongUrlHandler wrongUrlHandler;

    private CancelableScheduler scheduler;

    private PacketHandler packetHandler;
    private HeartbeatHandler heartbeatHandler;
    private SSLContext sslContext;
    private Configuration configuration;

    public void start(Configuration configuration, final NamespacesHub namespacesHub) {
        this.configuration = configuration;
        scheduler = new CancelableScheduler(configuration.getHeartbeatThreadPoolSize());

        ackManager = new AckManager(scheduler);

        JsonSupport jsonSupport = configuration.getJsonSupport();
        Encoder encoder = new Encoder(configuration, jsonSupport);
        Decoder decoder = new Decoder(jsonSupport, ackManager);

        heartbeatHandler = new HeartbeatHandler(configuration, scheduler);
        PacketListener packetListener = new PacketListener(heartbeatHandler, ackManager, namespacesHub);

        String connectPath = configuration.getContext() + "/" + protocol + "/";

        boolean isSsl = configuration.getKeyStore() != null;
        if (isSsl) {
            try {
                sslContext = createSSLContext(configuration.getKeyStore(), configuration.getKeyStorePassword());
            } catch (Exception e) {
                throw new IllegalStateException(e);
            }
        }

        packetHandler = new PacketHandler(packetListener, decoder, namespacesHub);
        authorizeHandler = new AuthorizeHandler(connectPath, scheduler, configuration, namespacesHub);

        StoreFactory factory = configuration.getStoreFactory();
        factory.init(namespacesHub, jsonSupport);

        xhrPollingTransport = new XHRPollingTransport(connectPath, ackManager, this, scheduler, authorizeHandler, configuration);
        webSocketTransport = new WebSocketTransport(connectPath, isSsl, ackManager, this, authorizeHandler, heartbeatHandler, factory);
        flashSocketTransport = new FlashSocketTransport(connectPath, isSsl, ackManager, this, authorizeHandler, heartbeatHandler, factory);

        resourceHandler = new ResourceHandler(configuration.getContext());
        encoderHandler = new EncoderHandler(encoder);
        wrongUrlHandler = new WrongUrlHandler();
    }

    public Collection<SocketIOClient> getAllClients() {
        // TODO refactor to transport registry
        Iterable<SocketIOClient> xhrClients = xhrPollingTransport.getAllClients();
        Iterable<SocketIOClient> webSocketClients = webSocketTransport.getAllClients();
        Iterable<SocketIOClient> flashSocketClients = flashSocketTransport.getAllClients();
        CompositeIterable<SocketIOClient> mainIterable = new CompositeIterable<SocketIOClient>(xhrClients, webSocketClients, flashSocketClients);
        return new IterableCollection<SocketIOClient>(mainIterable);
    }

    @Override
    protected void initChannel(Channel ch) throws Exception {
        ChannelPipeline pipeline = ch.pipeline();
        boolean isFlashTransport = configuration.getTransports().contains(FlashSocketTransport.NAME);
        if (isFlashTransport) {
            pipeline.addLast(FLASH_POLICY_HANDLER, flashPolicyHandler);
        }

        if (sslContext != null) {
            SSLEngine engine = sslContext.createSSLEngine();
            engine.setUseClientMode(false);
            pipeline.addLast(SSL_HANDLER, new SslHandler(engine));
        }

        pipeline.addLast(HTTP_REQUEST_DECODER, new HttpRequestDecoder());
        pipeline.addLast(HTTP_AGGREGATOR, new HttpObjectAggregator(configuration.getMaxHttpContentLength()));
        pipeline.addLast(HTTP_ENCODER, new HttpResponseEncoder());

        if (isFlashTransport) {
            pipeline.addLast(RESOURCE_HANDLER, resourceHandler);
        }
        pipeline.addLast(PACKET_HANDLER, packetHandler);

        pipeline.addLast(AUTHORIZE_HANDLER, authorizeHandler);
        pipeline.addLast(XHR_POLLING_TRANSPORT, xhrPollingTransport);
        pipeline.addLast(WEB_SOCKET_TRANSPORT, webSocketTransport);
        pipeline.addLast(FLASH_SOCKET_TRANSPORT, flashSocketTransport);

        pipeline.addLast(SOCKETIO_ENCODER, encoderHandler);

        pipeline.addLast(WRONG_URL_HANDLER, wrongUrlHandler);
    }

    private SSLContext createSSLContext(InputStream keyStoreFile, String keyStoreFilePassword) throws Exception {
        String algorithm = Security.getProperty("ssl.KeyManagerFactory.algorithm");
        if (algorithm == null) {
            algorithm = "SunX509";
        }

        KeyStore ks = KeyStore.getInstance("JKS");
        ks.load(keyStoreFile, keyStoreFilePassword.toCharArray());

        KeyManagerFactory kmf = KeyManagerFactory.getInstance(algorithm);
        kmf.init(ks, keyStoreFilePassword.toCharArray());

        SSLContext serverContext = SSLContext.getInstance("TLS");
        serverContext.init(kmf.getKeyManagers(), null, null);
        return serverContext;
    }

    public void onDisconnect(MainBaseClient client) {
        heartbeatHandler.onDisconnect(client);
        ackManager.onDisconnect(client);
        xhrPollingTransport.onDisconnect(client);
        webSocketTransport.onDisconnect(client);
        flashSocketTransport.onDisconnect(client);
        authorizeHandler.onDisconnect(client);
        configuration.getStoreFactory().onDisconnect(client);
        log.debug("Client with sessionId: {} disconnected", client.getSessionId());
    }

    public void stop() {
        StoreFactory factory = configuration.getStoreFactory();
        factory.shutdown();
        scheduler.shutdown();
    }

}
