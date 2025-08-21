/**
 * Copyright (c) 2012-2023 Nikita Koksharov
 *
 * <p>Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file
 * except in compliance with the License. You may obtain a copy of the License at
 *
 * <p>http://www.apache.org/licenses/LICENSE-2.0
 *
 * <p>Unless required by applicable law or agreed to in writing, software distributed under the
 * License is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either
 * express or implied. See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio.listener;

import java.util.List;

import com.corundumstudio.socketio.SocketIOClient;

import io.netty.channel.ChannelHandlerContext;

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
