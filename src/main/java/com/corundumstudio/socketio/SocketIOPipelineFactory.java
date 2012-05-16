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

import java.util.UUID;

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
import com.corundumstudio.socketio.transport.WebSocketTransport;
import com.corundumstudio.socketio.transport.XHRPollingTransport;

public class SocketIOPipelineFactory implements ChannelPipelineFactory, Disconnectable {

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final int protocol = 1;

    private final AuthorizeHandler authorizeHandler;
    private final XHRPollingTransport xhrPollingTransport;
    private final WebSocketTransport webSocketTransport;
    private final SocketIOEncoder socketIOEncoder;

    private final SocketIOListener socketIOHandler;
    private final CancelableScheduler<UUID> scheduler;

	private final PacketHandler packetHandler;

    public SocketIOPipelineFactory(Configuration configuration) {
        this.socketIOHandler = configuration.getListener();
        scheduler = new CancelableScheduler<UUID>(configuration.getHeartbeatThreadPoolSize());

        ObjectMapper objectMapper = configuration.getObjectMapper();
        Encoder encoder = new Encoder(objectMapper);
        Decoder decoder = new Decoder(objectMapper);

        HeartbeatHandler heartbeatHandler = new HeartbeatHandler(configuration, scheduler);
        PacketListener packetListener = new PacketListener(socketIOHandler, this, heartbeatHandler);

        String connectPath = configuration.getContext() + "/" + protocol + "/";

        packetHandler = new PacketHandler(packetListener, decoder);
        authorizeHandler = new AuthorizeHandler(connectPath, socketIOHandler, scheduler, configuration);
        xhrPollingTransport = new XHRPollingTransport(connectPath, this, scheduler, heartbeatHandler, authorizeHandler, configuration);
        webSocketTransport = new WebSocketTransport(connectPath, this, authorizeHandler);
        socketIOEncoder = new SocketIOEncoder(objectMapper, encoder);
    }

    public ChannelPipeline getPipeline() throws Exception {
        ChannelPipeline pipeline = pipeline();

        pipeline.addLast("decoder", new HttpRequestDecoder());
        pipeline.addLast("aggregator", new HttpChunkAggregator(65536));
        pipeline.addLast("encoder", new HttpResponseEncoder());

        pipeline.addLast("packetHandler", packetHandler);

        pipeline.addLast("authorizeHandler", authorizeHandler);
        pipeline.addLast("xhrPollingTransport", xhrPollingTransport);
        pipeline.addLast("webSocketTransport", webSocketTransport);

        pipeline.addLast("socketioEncoder", socketIOEncoder);

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
