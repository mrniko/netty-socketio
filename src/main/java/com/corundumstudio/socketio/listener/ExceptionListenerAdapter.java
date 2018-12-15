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
package com.corundumstudio.socketio.listener;

import com.corundumstudio.socketio.HttpRequestSignature;
import com.corundumstudio.socketio.SocketIOClient;
import io.netty.channel.ChannelHandlerContext;

import java.util.List;

/**
 * Base callback exceptions listener
 *
 *
 */
public abstract class ExceptionListenerAdapter implements ExceptionListener {

    @Override
    public void onEventException(Exception e, List<Object> data, SocketIOClient client) {
    }

    @Override
    public void onHttpException(Exception e, HttpRequestSignature signature) {
    }

    @Override
    public void onDisconnectException(Exception e, SocketIOClient client) {
    }

    @Override
    public void onConnectException(Exception e, SocketIOClient client) {
    }

    @Override
    public boolean exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
        return false;
    }

}
