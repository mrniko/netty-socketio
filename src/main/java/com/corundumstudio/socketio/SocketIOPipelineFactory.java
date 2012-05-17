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

import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelPipelineFactory;
import org.jboss.netty.handler.codec.http.HttpChunkAggregator;
import org.jboss.netty.handler.codec.http.HttpRequestDecoder;
import org.jboss.netty.handler.codec.http.HttpResponseEncoder;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.parser.Decoder;
import com.corundumstudio.socketio.parser.Encoder;
import com.corundumstudio.socketio.scheduler.CancelableScheduler;
import com.corundumstudio.socketio.transport.WebSocketTransport;
import com.corundumstudio.socketio.transport.XHRPollingTransport;

public class SocketIOPipelineFactory implements ChannelPipelineFactory, Disconnectable {

    protected static final String SOCKETIO_ENCODER = "socketioEncoder";
	protected static final String WEB_SOCKET_TRANSPORT = "webSocketTransport";
	protected static final String XHR_POLLING_TRANSPORT = "xhrPollingTransport";
	protected static final String AUTHORIZE_HANDLER = "authorizeHandler";
	protected static final String PACKET_HANDLER = "packetHandler";
	protected static final String HTTP_ENCODER = "encoder";
	protected static final String HTTP_AGGREGATOR = "aggregator";
	protected static final String HTTP_REQUEST_DECODER = "decoder";

	private final Logger log = LoggerFactory.getLogger(getClass());

    private final int protocol = 1;

    private AuthorizeHandler authorizeHandler;
    private XHRPollingTransport xhrPollingTransport;
    private WebSocketTransport webSocketTransport;
    private SocketIOEncoder socketIOEncoder;

    private SocketIOListener socketIOHandler;
    private CancelableScheduler scheduler;

	private PacketHandler packetHandler;

    public void start(Configuration configuration) {
        this.socketIOHandler = configuration.getListener();
        scheduler = new CancelableScheduler(configuration.getHeartbeatThreadPoolSize());

        ObjectMapper objectMapper = configuration.getObjectMapper();
        Encoder encoder = new Encoder(objectMapper);
        Decoder decoder = new Decoder(objectMapper);

        HeartbeatHandler heartbeatHandler = new HeartbeatHandler(configuration, scheduler);
        PacketListener packetListener = new PacketListener(socketIOHandler, this, heartbeatHandler);

        String connectPath = configuration.getContext() + "/" + protocol + "/";

        packetHandler = new PacketHandler(packetListener, decoder);
        authorizeHandler = new AuthorizeHandler(connectPath, socketIOHandler, scheduler, configuration);
        xhrPollingTransport = new XHRPollingTransport(connectPath, this, scheduler, authorizeHandler, configuration);
        webSocketTransport = new WebSocketTransport(connectPath, this, authorizeHandler, heartbeatHandler);
        socketIOEncoder = new SocketIOEncoder(objectMapper, encoder);
    }

    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = pipeline();

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

    public void onDisconnect(SocketIOClient client) {
        log.debug("Client with sessionId: {} disconnected by client request", client.getSessionId());
        xhrPollingTransport.onDisconnect(client);
        webSocketTransport.onDisconnect(client);
        authorizeHandler.onDisconnect(client);
        socketIOHandler.onDisconnect(client);
    }

    public void stop() {
    	scheduler.shutdown();
    }

}
