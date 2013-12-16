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

import io.netty.channel.ChannelPipeline;
import io.netty.channel.ChannelHandler.Sharable;

import com.corundumstudio.socketio.DisconnectableHub;
import com.corundumstudio.socketio.SocketIOChannelInitializer;
import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.ack.AckManager;
import com.corundumstudio.socketio.handler.AuthorizeHandler;
import com.corundumstudio.socketio.handler.HeartbeatHandler;
import com.corundumstudio.socketio.store.StoreFactory;

@Sharable
public class FlashSocketTransport extends WebSocketTransport {

    public static final String NAME = "flashsocket";

    public FlashSocketTransport(String connectPath, boolean isSsl, AckManager ackManager,
            DisconnectableHub disconnectable, AuthorizeHandler authorizeHandler,
            HeartbeatHandler heartbeatHandler, StoreFactory storeFactory) {
        super(connectPath, isSsl, ackManager, disconnectable, authorizeHandler, heartbeatHandler, storeFactory);
        path = connectPath + NAME;
    }

    @Override
    protected Transport getTransport() {
        return Transport.FLASHSOCKET;
    }

    @Override
    protected void removeHandler(ChannelPipeline pipeline) {
        pipeline.remove(SocketIOChannelInitializer.WEB_SOCKET_TRANSPORT);
    }

}
