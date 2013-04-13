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
package com.corundumstudio.socketio.transport;

import org.jboss.netty.channel.ChannelPipeline;
import org.jboss.netty.channel.ChannelHandler.Sharable;

import com.corundumstudio.socketio.DisconnectableHub;
import com.corundumstudio.socketio.HeartbeatHandler;
import com.corundumstudio.socketio.SocketIOPipelineFactory;
import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.ack.AckManager;
import com.corundumstudio.socketio.handler.AuthorizeHandler;

@Sharable
public class FlashSocketTransport extends WebSocketTransport {

    public static final String NAME = "flashsocket";

    public FlashSocketTransport(String connectPath, boolean isSsl, AckManager ackManager,
            DisconnectableHub disconnectable, AuthorizeHandler authorizeHandler,
            HeartbeatHandler heartbeatHandler) {
        super(connectPath, isSsl, ackManager, disconnectable, authorizeHandler, heartbeatHandler);
        path = connectPath + NAME;
    }
    
    @Override
    protected Transport getTransport() {
        return Transport.FLASHSOCKET;
    }

    @Override
    protected void removeHandler(ChannelPipeline pipeline) {
        pipeline.remove(SocketIOPipelineFactory.WEB_SOCKET_TRANSPORT);
    }

}
