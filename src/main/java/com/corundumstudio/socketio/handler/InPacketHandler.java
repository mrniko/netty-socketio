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
package com.corundumstudio.socketio.handler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.AuthTokenResult;
import com.corundumstudio.socketio.listener.ExceptionListener;
import com.corundumstudio.socketio.messages.PacketsMessage;
import com.corundumstudio.socketio.namespace.Namespace;
import com.corundumstudio.socketio.namespace.NamespacesHub;
import com.corundumstudio.socketio.protocol.ConnPacket;
import com.corundumstudio.socketio.protocol.EngineIOVersion;
import com.corundumstudio.socketio.protocol.Packet;
import com.corundumstudio.socketio.protocol.PacketDecoder;
import com.corundumstudio.socketio.protocol.PacketType;
import com.corundumstudio.socketio.transport.NamespaceClient;

import io.netty.buffer.ByteBuf;
import io.netty.channel.ChannelHandler.Sharable;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.util.CharsetUtil;

@Sharable
public class InPacketHandler extends SimpleChannelInboundHandler<PacketsMessage> {

    private static final Logger log = LoggerFactory.getLogger(InPacketHandler.class);

    private final PacketListener packetListener;
    private final PacketDecoder decoder;
    private final NamespacesHub namespacesHub;
    private final ExceptionListener exceptionListener;

    public InPacketHandler(PacketListener packetListener, PacketDecoder decoder, NamespacesHub namespacesHub, ExceptionListener exceptionListener) {
        super();
        this.packetListener = packetListener;
        this.decoder = decoder;
        this.namespacesHub = namespacesHub;
        this.exceptionListener = exceptionListener;
    }

    @Override
    protected void channelRead0(io.netty.channel.ChannelHandlerContext ctx, PacketsMessage message)
                throws Exception {
        ByteBuf content = message.getContent();
        ClientHead client = message.getClient();

        if (log.isTraceEnabled()) {
            log.trace("In message: {} sessionId: {}", content.toString(CharsetUtil.UTF_8), client.getSessionId());
        }
        
        int packetsProcessed = 0;
        while (content.isReadable()) {
            try {
                Packet packet = decoder.decodePackets(content, client);
                packetsProcessed++;

                if (log.isDebugEnabled()) {
                    log.debug("Decoded packet: type={}, subType={}, namespace={}, client={}, hasAttachments={}", 
                             packet.getType(), packet.getSubType(), packet.getNsp(), 
                             client.getSessionId(), packet.hasAttachments());
                }

                Namespace ns = namespacesHub.get(packet.getNsp());
                if (ns == null) {
                    if (packet.getSubType() == PacketType.CONNECT) {
                        if (log.isDebugEnabled()) {
                            log.debug("Sending error response for invalid namespace: {} to client: {}", 
                                     packet.getNsp(), client.getSessionId());
                        }
                        Packet p = new Packet(PacketType.MESSAGE, client.getEngineIOVersion());
                        p.setSubType(PacketType.ERROR);
                        p.setNsp(packet.getNsp());
                        p.setData("Invalid namespace");
                        client.send(p);
                        return;
                    }
                    log.debug("Can't find namespace for endpoint: {}, sessionId: {} probably it was removed.", packet.getNsp(), client.getSessionId());
                    return;
                }

                if (packet.getSubType() == PacketType.CONNECT) {
                    if (log.isDebugEnabled()) {
                        log.debug("Processing CONNECT packet for namespace: {} from client: {}, Engine.IO version: {}", 
                                 ns.getName(), client.getSessionId(), client.getEngineIOVersion());
                    }
                    
                    client.addNamespaceClient(ns);
                    NamespaceClient nClient = client.getChildClient(ns);
                    //:TODO lyjnew client namespace send connect packet 0+namespace  socket io v4
                    // https://socket.io/docs/v4/socket-io-protocol/#connection-to-a-namespace
                    if (EngineIOVersion.V4.equals(client.getEngineIOVersion())) {
                        handleV4Connect(packet, client, ns, nClient);
                    }
                }

                NamespaceClient nClient = client.getChildClient(ns);
                if (nClient == null) {
                    log.debug("Can't find namespace client in namespace: {}, sessionId: {} probably it was disconnected.", ns.getName(), client.getSessionId());
                    return;
                }
                if (packet.hasAttachments() && !packet.isAttachmentsLoaded()) {
                    if (log.isDebugEnabled()) {
                        log.debug("Packet has unloaded attachments, deferring processing for client: {}, namespace: {}", 
                                 client.getSessionId(), ns.getName());
                    }
                    return;
                }
                packetListener.onPacket(packet, nClient, message.getTransport());
                if (log.isDebugEnabled()) {
                    log.debug("Successfully processed packet for client: {}, namespace: {}", 
                             client.getSessionId(), ns.getName());
                }
            } catch (Exception ex) {
                String c = content.toString(CharsetUtil.UTF_8);
                log.error("Error during data processing. Client sessionId: " + client.getSessionId() + ", data: " + c, ex);
                throw ex;
            }
        }
        
        if (log.isDebugEnabled()) {
            log.debug("Completed processing {} packets for client: {}", packetsProcessed, client.getSessionId());
        }
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
        if (log.isDebugEnabled()) {
            log.debug("Exception caught in InPacketHandler for channel: {}, exception type: {}, message: {}", 
                     ctx.channel().id(), e.getClass().getSimpleName(), e.getMessage());
        }
        
        boolean handled = exceptionListener.exceptionCaught(ctx, e);
        
        if (log.isDebugEnabled()) {
            log.debug("Exception (handled: {}) by custom exception listener for channel: {}",
                    handled, ctx.channel().id());
        }
        
        if (!handled) {
            if (log.isDebugEnabled()) {
                log.debug("Delegating exception handling to parent handler for channel: {}", ctx.channel().id());
            }
            super.exceptionCaught(ctx, e);
        }
    }

    private void handleV4Connect(Packet packet, ClientHead client, Namespace ns, NamespaceClient nClient) {
        if (log.isDebugEnabled()) {
            log.debug("Starting Engine.IO v4 connect handling for client: {}, namespace: {}, hasAuthData: {}", 
                     client.getSessionId(), ns.getName(), packet.getData() != null);
        }
        
        // Check for an auth token
        if (packet.getData() != null) {
            final Object authData = packet.getData();
            
            if (log.isDebugEnabled()) {
                log.debug("Processing authentication data for client: {}, namespace: {}, authData type: {}", 
                         client.getSessionId(), ns.getName(), authData.getClass().getSimpleName());
            }
            
            client.getHandshakeData().setAuthToken(authData);
            
            // Call all authTokenListeners to see if one denies it
            final AuthTokenResult allowAuth = ns.onAuthData(nClient, authData);
            if (!allowAuth.isSuccess()) {
                if (log.isDebugEnabled()) {
                    log.debug("Authentication failed for client: {}, namespace: {}, sending error response", 
                             client.getSessionId(), ns.getName());
                }
                
                Packet p = new Packet(PacketType.MESSAGE, client.getEngineIOVersion());
                p.setSubType(PacketType.ERROR);
                p.setNsp(packet.getNsp());
                final Object errorData = allowAuth.getErrorData();
                if (errorData != null) {
                    p.setData(errorData);
                }
                client.send(p);
                return;
            }
        } else {
            if (log.isDebugEnabled()) {
                log.debug("No authentication data provided for client: {}, namespace: {}, proceeding with connection", 
                         client.getSessionId(), ns.getName());
            }
        }
        Packet p = new Packet(PacketType.MESSAGE, client.getEngineIOVersion());
        p.setSubType(PacketType.CONNECT);
        p.setNsp(packet.getNsp());
        p.setData(new ConnPacket(client.getSessionId()));
        client.send(p);
        if (log.isDebugEnabled()) {
            log.debug("Completed Engine.IO v4 connect handling for client: {}, namespace: {}", 
                     client.getSessionId(), ns.getName());
        }
    }

}
