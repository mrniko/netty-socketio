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

import io.netty.channel.ChannelHandlerContext;

import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.SocketIOClient;

public class DefaultExceptionListener extends ExceptionListenerAdapter {

    private static final Logger log = LoggerFactory.getLogger(DefaultExceptionListener.class);

    @Override
    public void onEventException(Exception e, List<Object> args, SocketIOClient client) {
        log.error(e.getMessage(), e);
    }

    @Override
    public void onDisconnectException(Exception e, SocketIOClient client) {
        log.error(e.getMessage(), e);
    }

    @Override
    public void onConnectException(Exception e, SocketIOClient client) {
        log.error(e.getMessage(), e);
    }

    public boolean exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception {
        log.error(e.getMessage(), e);
        return true;
    }

}
