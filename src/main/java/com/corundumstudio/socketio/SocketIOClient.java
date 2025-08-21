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
package com.corundumstudio.socketio;

import java.net.SocketAddress;
import java.util.Set;
import java.util.UUID;

import com.corundumstudio.socketio.protocol.EngineIOVersion;
import com.corundumstudio.socketio.protocol.Packet;
import com.corundumstudio.socketio.store.Store;


/**
 * Fully thread-safe.
 *
 */
public interface SocketIOClient extends ClientOperations, Store {

    /**
     * Handshake data used during client connection
     *
     * @return HandshakeData
     */
    HandshakeData getHandshakeData();

    /**
     * Current client transport protocol
     *
     * @return transport protocol
     */
    Transport getTransport();

    /**
     * Engine IO Protocol version
     * @return
     */
    EngineIOVersion getEngineIOVersion();

    /**
     * Returns true if and only if the I/O thread will perform the requested write operation immediately.
     * Any write requests made when this method returns false are queued until the I/O thread is ready to process the queued write requests.
     * @return
     */
    boolean isWritable();

    /**
     * Send event with ack callback
     *
     * @param name - event name
     * @param data - event data
     * @param ackCallback - ack callback
     */
    void sendEvent(String name, AckCallback<?> ackCallback, Object... data);

    /**
     * Send packet with ack callback
     *
     * @param packet - packet to send
     * @param ackCallback - ack callback
     */
    void send(Packet packet, AckCallback<?> ackCallback);

    /**
     * Client namespace
     *
     * @return - namespace
     */
    SocketIONamespace getNamespace();

    /**
     * Client session id, uses {@link UUID} object
     *
     * @return - session id
     */
    UUID getSessionId();

    /**
     * Get client remote address
     *
     * @return remote address
     */
    SocketAddress getRemoteAddress();

    /**
     * Check is underlying channel open
     *
     * @return <code>true</code> if channel open, otherwise <code>false</code>
     */
    boolean isChannelOpen();

    /**
     * Join client to room
     *
     * @param room - name of room
     */
    void joinRoom(String room);

    /**
     * Join client to rooms
     *
     * @param rooms - names of rooms
     */
    void joinRooms(Set<String> rooms);

    /**
     * Leave client from room
     *
     * @param room - name of room
     */
    void leaveRoom(String room);

    /**
     * Leave client from rooms
     *
     * @param rooms - names of rooms
     */
    void leaveRooms(Set<String> rooms);

    /**
     * Get all rooms a client is joined in.
     *
     * @return name of rooms
     */
    Set<String> getAllRooms();

    /**
     * Get current room Size (contain in cluster)
     *
     * @param room - name of room
     *
     * @return int
     */
    int getCurrentRoomSize(String room);

}
