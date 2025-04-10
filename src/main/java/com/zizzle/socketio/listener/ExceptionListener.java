/**
 * Copyright (c) 2012-2023 Nikita Koksharov
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
package com.zizzle.socketio.listener;

import io.netty.channel.ChannelHandlerContext;

import java.util.List;

import com.zizzle.socketio.SocketIOClient;

public interface ExceptionListener {

    void onEventException(Exception e, List<Object> args, SocketIOClient client);

    void onDisconnectException(Exception e, SocketIOClient client);

    void onConnectException(Exception e, SocketIOClient client);

    @Deprecated
    void onPingException(Exception e, SocketIOClient client);

    void onPongException(Exception e, SocketIOClient client);

    boolean exceptionCaught(ChannelHandlerContext ctx, Throwable e) throws Exception;

    void onAuthException(Throwable e, SocketIOClient client);
}
