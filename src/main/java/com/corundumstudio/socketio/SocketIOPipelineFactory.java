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

import static org.jboss.netty.channel.Channels.pipeline;

import java.io.FileInputStream;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.Security;

import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLEngine;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.jboss.netty.handler.ssl.SslHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.namespace.NamespacesHub;
import com.corundumstudio.socketio.parser.Decoder;
import com.corundumstudio.socketio.parser.Encoder;
import com.corundumstudio.socketio.parser.JsonSupport;
import com.corundumstudio.socketio.scheduler.CancelableScheduler;
import com.corundumstudio.socketio.transport.BaseClient;
import com.corundumstudio.socketio.transport.WebSocketTransport;
import com.corundumstudio.socketio.transport.XHRPollingTransport;

public class SocketIOPipelineFactory implements ChannelPipelineFactory, DisconnectableHub {

    protected static final String SOCKETIO_ENCODER = "socketioEncoder";
    protected static final String WEB_SOCKET_TRANSPORT = "webSocketTransport";
    protected static final String XHR_POLLING_TRANSPORT = "xhrPollingTransport";
    protected static final String AUTHORIZE_HANDLER = "authorizeHandler";
    protected static final String PACKET_HANDLER = "packetHandler";
    protected static final String HTTP_ENCODER = "encoder";
    protected static final String HTTP_AGGREGATOR = "aggregator";
    protected static final String HTTP_REQUEST_DECODER = "decoder";
    protected static final String SSL_HANDLER = "ssl";

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final int protocol = 1;

    private AckManager ackManager;

    private AuthorizeHandler authorizeHandler;
    private XHRPollingTransport xhrPollingTransport;
    private WebSocketTransport webSocketTransport;
    private SocketIOEncoder socketIOEncoder;

    private CancelableScheduler scheduler;

    private PacketHandler packetHandler;
    private HeartbeatHandler heartbeatHandler;
    private SSLContext sslContext;

    public void start(Configuration configuration, NamespacesHub namespacesHub) {
        scheduler = new CancelableScheduler(configuration.getHeartbeatThreadPoolSize());

        JsonSupport jsonSupport = configuration.getJsonSupport();
        Encoder encoder = new Encoder(jsonSupport);
        Decoder decoder = new Decoder(jsonSupport);

        ackManager = new AckManager(scheduler);
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
        xhrPollingTransport = new XHRPollingTransport(connectPath, ackManager, this, scheduler, authorizeHandler, configuration);
        webSocketTransport = new WebSocketTransport(connectPath, isSsl, ackManager, this, authorizeHandler, heartbeatHandler);
        socketIOEncoder = new SocketIOEncoder(encoder);
    }

    public Iterable<SocketIOClient> getAllClients() {
        Iterable<SocketIOClient> xhrClients = xhrPollingTransport.getAllClients();
        Iterable<SocketIOClient> webSocketClients = webSocketTransport.getAllClients();
        return new CompositeIterable<SocketIOClient>(xhrClients, webSocketClients);
    }

    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = pipeline();

        if (sslContext != null) {
            SSLEngine engine = sslContext.createSSLEngine();
            engine.setUseClientMode(false);
            pipeline.addLast(SSL_HANDLER, new SslHandler(engine));
        }

        pipeline.addLast(HTTP_REQUEST_DECODER, new HttpRequestDecoder());
        pipeline.addLast(HTTP_AGGREGATOR, new HttpChunkAggregator(65536));
        pipeline.addLast(HTTP_ENCODER, new HttpResponseEncoder());

        pipeline.addLast(PACKET_HANDLER, packetHandler);

        pipeline.addLast(AUTHORIZE_HANDLER, authorizeHandler);
        pipeline.addLast(XHR_POLLING_TRANSPORT, xhrPollingTransport);
        pipeline.addLast(WEB_SOCKET_TRANSPORT, webSocketTransport);

        pipeline.addLast(SOCKETIO_ENCODER, socketIOEncoder);

        return pipeline;
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

    public void onDisconnect(BaseClient client) {
        log.debug("Client with sessionId: {} disconnected", client.getSessionId());
        heartbeatHandler.onDisconnect(client);
        ackManager.onDisconnect(client);
        xhrPollingTransport.onDisconnect(client);
        webSocketTransport.onDisconnect(client);
        authorizeHandler.onDisconnect(client);
    }

    public void stop() {
        scheduler.shutdown();
    }

}
