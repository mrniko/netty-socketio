/**
 * Copyright (c) 2012-2019 Nikita Koksharov
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio;

import com.corundumstudio.socketio.protocol.Packet;

import java.util.Collection;

/**
 * broadcast interface
 */
public interface BroadcastOperations extends ClientOperations {

    Collection<SocketIOClient> getClients();

    /**
     * {@link Packet#attachments} needs to be filled when sending byte[].
     * Using {@link io.netty.buffer.Unpooled#wrappedBuffer(byte[])} to
     * fill byte[] into {@link Packet#attachments} is the recommended way.
     * Before using {@link Packet#addAttachment(io.netty.buffer.ByteBuf)},
     * be sure to initialize the number of attachments with
     * {@link Packet#initAttachments(int)})}
     *
     * @param packet
     * @param ackCallback
     * @param <T>
     */
    <T> void send(Packet packet, BroadcastAckCallback<T> ackCallback);

    void sendEvent(String name, SocketIOClient excludedClient, Object... data);

    <T> void sendEvent(String name, Object data, BroadcastAckCallback<T> ackCallback);

    <T> void sendEvent(String name, Object data, SocketIOClient excludedClient, BroadcastAckCallback<T> ackCallback);

}
