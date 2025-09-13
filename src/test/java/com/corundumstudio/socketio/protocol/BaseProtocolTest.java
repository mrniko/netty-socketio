/**
 * Copyright (c) 2012-2025 Nikita Koksharov
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
package com.corundumstudio.socketio.protocol;

import java.util.Arrays;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.mockito.MockitoAnnotations;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.Unpooled;

/**
 * Base class for protocol tests providing common utilities and setup
 */
public abstract class BaseProtocolTest {

    protected static final String DEFAULT_NAMESPACE = "/";
    protected static final String ADMIN_NAMESPACE = "/admin";
    protected static final String CUSTOM_NAMESPACE = "/custom";

    protected static final String TEST_EVENT_NAME = "testEvent";
    protected static final String TEST_MESSAGE = "Hello World";
    protected static final Long TEST_ACK_ID = 123L;
    protected static final UUID TEST_SID = UUID.randomUUID();

    protected static final byte[] TEST_BINARY_DATA = {0x01, 0x02, 0x03, 0x04};
    protected static final String[] TEST_UPGRADES = {"websocket", "polling"};
    protected static final int TEST_PING_INTERVAL = 25000;
    protected static final int TEST_PING_TIMEOUT = 5000;

    private AutoCloseable closeableMocks;

    @BeforeEach
    public void setUp() {
        closeableMocks = MockitoAnnotations.openMocks(this);
    }

    @AfterEach
    public void tearDown() throws Exception {
        closeableMocks.close();
    }

    /**
     * Create a test packet with basic configuration
     */
    protected Packet createTestPacket(PacketType type) {
        Packet packet = new Packet(type);
        packet.setNsp(DEFAULT_NAMESPACE);
        return packet;
    }

    /**
     * Create a test packet with event subtype
     */
    protected Packet createEventPacket(String eventName, Object data) {
        Packet packet = new Packet(PacketType.MESSAGE);
        packet.setSubType(PacketType.EVENT);
        packet.setName(eventName);
        packet.setData(data);
        packet.setNsp(DEFAULT_NAMESPACE);
        return packet;
    }

    /**
     * Create a test packet with acknowledgment
     */
    protected Packet createAckPacket(String namespace, Long ackId, Object data) {
        Packet packet = new Packet(PacketType.MESSAGE);
        packet.setSubType(PacketType.ACK);
        packet.setAckId(ackId);
        packet.setData(data);
        packet.setNsp(namespace);
        return packet;
    }

    /**
     * Create a test packet with binary attachments
     */
    protected Packet createBinaryPacket(PacketType subType, String namespace, Object data, int attachmentsCount) {
        Packet packet = new Packet(PacketType.MESSAGE);
        packet.setSubType(subType);
        packet.setData(data);
        packet.setNsp(namespace);
        packet.initAttachments(attachmentsCount);

        for (int i = 0; i < attachmentsCount; i++) {
            byte[] attachmentData = Arrays.copyOf(TEST_BINARY_DATA, TEST_BINARY_DATA.length);
            attachmentData[0] = (byte) i; // Make each attachment unique
            packet.addAttachment(Unpooled.wrappedBuffer(attachmentData));
        }

        return packet;
    }

    /**
     * Create a ByteBuf with test data
     */
    protected ByteBuf createTestByteBuf(String data) {
        return Unpooled.copiedBuffer(data.getBytes());
    }

    /**
     * Create a ByteBuf with binary data
     */
    protected ByteBuf createBinaryByteBuf(byte[] data) {
        return Unpooled.wrappedBuffer(data);
    }

    /**
     * Create test event data
     */
    protected Event createTestEvent(String name, Object... args) {
        return new Event(name, Arrays.asList(args));
    }

    /**
     * Create test acknowledgment arguments
     */
    protected AckArgs createTestAckArgs(Object... args) {
        return new AckArgs(Arrays.asList(args));
    }

    /**
     * Create test authentication packet
     */
    protected AuthPacket createTestAuthPacket() {
        return new AuthPacket(TEST_SID, TEST_UPGRADES, TEST_PING_INTERVAL, TEST_PING_TIMEOUT);
    }

    /**
     * Create test connection packet
     */
    protected ConnPacket createTestConnPacket() {
        return new ConnPacket(TEST_SID);
    }

    /**
     * Helper method to convert ByteBuf to string for assertions
     */
    protected String byteBufToString(ByteBuf buf) {
        byte[] bytes = new byte[buf.readableBytes()];
        buf.getBytes(buf.readerIndex(), bytes);
        return new String(bytes);
    }

    /**
     * Helper method to reset ByteBuf reader index
     */
    protected void resetReaderIndex(ByteBuf buf) {
        buf.readerIndex(0);
    }
}
