/**
 * Copyright (c) 2012-2019 Nikita Koksharov
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

import com.corundumstudio.socketio.protocol.Packet;

/**
 * Available client operations
 *
 */
public interface ClientOperations {

    /**
     * Send custom packet.
     * But {@link ClientOperations#sendEvent} method
     * usage is enough for most cases.
     * {@link Packet#attachments} needs to be filled when sending byte[].
     * Using {@link io.netty.buffer.Unpooled#wrappedBuffer(byte[])} to
     * fill byte[] into {@link Packet#attachments} is the recommended way.
     * Before using {@link Packet#addAttachment(io.netty.buffer.ByteBuf)},
     * be sure to initialize the number of attachments with
     * {@link Packet#initAttachments(int)})}
     *
     * @param packet - packet to send
     */
    void send(Packet packet);

    /**
     * Disconnect client
     *
     */
    void disconnect();

    /**
     * Send event
     *
     * @param name - event name
     * @param data - event data
     */
    void sendEvent(String name, Object ... data);

}
