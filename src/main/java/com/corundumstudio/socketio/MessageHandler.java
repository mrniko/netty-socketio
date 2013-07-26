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

import io.netty.channel.Channel;

import java.io.IOException;

import com.corundumstudio.socketio.messages.AuthorizeMessage;
import com.corundumstudio.socketio.messages.WebSocketPacketMessage;
import com.corundumstudio.socketio.messages.WebsocketErrorMessage;
import com.corundumstudio.socketio.messages.XHRErrorMessage;
import com.corundumstudio.socketio.messages.XHRNewChannelMessage;
import com.corundumstudio.socketio.messages.XHRPacketMessage;
import com.corundumstudio.socketio.messages.XHROutMessage;

public interface MessageHandler {

    void handle(XHRNewChannelMessage xhrNewChannelMessage, Channel channel) throws IOException;

    void handle(XHRPacketMessage xhrPacketMessage, Channel channel) throws IOException;

    void handle(XHROutMessage xhrPostMessage, Channel channel);

    void handle(AuthorizeMessage authorizeMessage, Channel channel) throws IOException;

    void handle(WebSocketPacketMessage webSocketPacketMessage, Channel channel) throws IOException;

    void handle(WebsocketErrorMessage websocketErrorMessage, Channel channel) throws IOException;

    void handle(XHRErrorMessage xhrErrorMessage, Channel channel) throws IOException;

}
