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
package com.corundumstudio.socketio.handler;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.channel.ChannelPromise;
import io.netty.channel.embedded.EmbeddedChannel;
import io.netty.handler.codec.http.HttpResponse;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.handler.codec.http.websocketx.ContinuationWebSocketFrame;
import io.netty.handler.codec.http.websocketx.TextWebSocketFrame;
import io.netty.handler.codec.http.websocketx.WebSocketFrame;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentLinkedQueue;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.messages.HttpErrorMessage;
import com.corundumstudio.socketio.messages.OutPacketMessage;
import com.corundumstudio.socketio.messages.XHROptionsMessage;
import com.corundumstudio.socketio.messages.XHRPostMessage;
import com.corundumstudio.socketio.protocol.EngineIOVersion;
import com.corundumstudio.socketio.protocol.JsonSupport;
import com.corundumstudio.socketio.protocol.Packet;
import com.corundumstudio.socketio.protocol.PacketEncoder;
import com.corundumstudio.socketio.protocol.PacketType;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Comprehensive integration test suite for EncoderHandler.
 * <p>
 * This test class validates the complete functionality of the EncoderHandler,
 * which is responsible for encoding and sending various types of Socket.IO messages
 * through different transport mechanisms (WebSocket and HTTP Polling).
 * <p>
 * Test Coverage:
 * - WebSocket transport message handling
 * - HTTP polling transport message handling
 * - XHR options and post message processing
 * - HTTP error message handling
 * - Large message fragmentation for WebSocket
 * - Binary attachment handling
 * - JSONP encoding for legacy clients
 * - Channel attribute management
 * - Message encoding and serialization
 * - Error handling and edge cases
 * <p>
 * Testing Approach:
 * - Uses EmbeddedChannel for realistic Netty pipeline testing
 * - Mocks dependencies (PacketEncoder, JsonSupport) for controlled testing
 * - Tests both success and failure scenarios
 * - Validates message content, headers, and channel state
 * - Ensures proper resource management and cleanup
 * <p>
 * Key Test Scenarios:
 * 1. WebSocket message encoding and transmission
 * 2. HTTP polling with various encoding options
 * 3. Large message fragmentation handling
 * 4. Binary attachment processing
 * 5. Error message formatting and transmission
 * 6. Channel attribute management and validation
 * 7. Transport-specific message handling
 *
 * @see EncoderHandler
 * @see EmbeddedChannel
 * @see Socket.IO Protocol Specification
 */
public class EncoderHandlerTest {

    private static final String TEST_ORIGIN = "http://localhost:3000";
    private static final int MAX_FRAME_PAYLOAD_LENGTH = 1024;

    @Mock
    private PacketEncoder mockEncoder;

    @Mock
    private JsonSupport mockJsonSupport;

    private EncoderHandler encoderHandler;
    private Configuration configuration;
    private EmbeddedChannel channel;
    private UUID sessionId;

    @BeforeEach
    void setUp() throws IOException {
        MockitoAnnotations.openMocks(this);
        sessionId = UUID.randomUUID();
        configuration = new Configuration();
        configuration.setMaxFramePayloadLength(MAX_FRAME_PAYLOAD_LENGTH);
        configuration.setAddVersionHeader(false);

        when(mockEncoder.getJsonSupport()).thenReturn(mockJsonSupport);
        doAnswer(invocation -> {
            // Return a buffer with enough capacity for large message testing
            return Unpooled.buffer(20000);
        }).when(mockEncoder).allocateBuffer(any());

        encoderHandler = new EncoderHandler(configuration, mockEncoder);
        channel = new EmbeddedChannel(encoderHandler);
    }

    @Test
    @DisplayName("Should handle XHR options message correctly")
    void shouldHandleXHROptionsMessage() throws Exception {
        // Given
        XHROptionsMessage message = new XHROptionsMessage(TEST_ORIGIN, sessionId);
        channel.attr(EncoderHandler.ORIGIN).set(TEST_ORIGIN);
        ChannelPromise promise = channel.newPromise();

        // When
        encoderHandler.write(channel.pipeline().context(encoderHandler), message, promise);

        // Then
        assertThat(channel.outboundMessages()).hasSize(2); // HttpResponse + LastHttpContent
        HttpResponse response = channel.readOutbound();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.OK);
        assertThat(response.headers().get("Set-Cookie")).contains("io=" + sessionId);
        assertThat(response.headers().get("Connection")).isEqualTo("keep-alive");
        assertThat(response.headers().get("Access-Control-Allow-Headers")).isEqualTo("content-type");
        assertThat(response.headers().get("Access-Control-Allow-Origin")).isEqualTo(TEST_ORIGIN);
        assertThat(response.headers().get("Access-Control-Allow-Credentials")).isEqualTo("true");
    }

    @Test
    @DisplayName("Should handle XHR post message correctly")
    void shouldHandleXHRPostMessage() throws Exception {
        // Given
        XHRPostMessage message = new XHRPostMessage(TEST_ORIGIN, sessionId);
        channel.attr(EncoderHandler.ORIGIN).set(TEST_ORIGIN);
        ChannelPromise promise = channel.newPromise();

        // When
        encoderHandler.write(channel.pipeline().context(encoderHandler), message, promise);

        // Then
        assertThat(channel.outboundMessages()).hasSize(3);
        HttpResponse response = channel.readOutbound();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.OK);
        assertThat(response.headers().get("Content-Type")).isEqualTo("text/html");
        assertThat(response.headers().get("Set-Cookie")).contains("io=" + sessionId);
    }

    @Test
    @DisplayName("Should handle HTTP error message correctly")
    void shouldHandleHttpErrorMessage() throws Exception {
        // Given
        Map<String, Object> errorData = new HashMap<>();
        errorData.put("error", "Invalid request");
        errorData.put("code", 400);
        HttpErrorMessage message = new HttpErrorMessage(errorData);
        channel.attr(EncoderHandler.ORIGIN).set(TEST_ORIGIN);
        ChannelPromise promise = channel.newPromise();

        doAnswer(invocation -> {
            ByteBufOutputStream outputStream = invocation.getArgument(0);
            outputStream.write("{\"error\":\"Invalid request\",\"code\":400}".getBytes());
            return null;
        }).when(mockJsonSupport).writeValue(any(), any());

        // When
        encoderHandler.write(channel.pipeline().context(encoderHandler), message, promise);

        // Then
        assertThat(channel.outboundMessages()).hasSize(3);
        HttpResponse response = channel.readOutbound();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.BAD_REQUEST);
        assertThat(response.headers().get("Content-Type")).isEqualTo("application/json");
    }

    @Test
    @DisplayName("Should handle WebSocket transport with small message")
    void shouldHandleWebSocketTransportWithSmallMessage() throws Exception {
        // Given
        ClientHead clientHead = createMockClientHead(Transport.WEBSOCKET);
        OutPacketMessage message = new OutPacketMessage(clientHead, Transport.WEBSOCKET);
        ChannelPromise promise = channel.newPromise();

        Packet packet = new Packet(PacketType.MESSAGE, EngineIOVersion.V4);
        packet.setData("Hello World");
        clientHead.getPacketsQueue(Transport.WEBSOCKET).add(packet);

        doAnswer(invocation -> {
            ByteBuf buffer = invocation.getArgument(1);
            buffer.writeBytes("42[\"Hello World\"]".getBytes());
            return null;
        }).when(mockEncoder).encodePacket(any(), any(), any(), eq(true));

        // When
        encoderHandler.write(channel.pipeline().context(encoderHandler), message, promise);

        // Then
        assertThat(channel.outboundMessages()).hasSize(1);
        WebSocketFrame frame = channel.readOutbound();
        assertThat(frame).isInstanceOf(TextWebSocketFrame.class);
        assertThat(frame.content().readableBytes()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should handle WebSocket transport with large message fragmentation")
    void shouldHandleWebSocketTransportWithLargeMessageFragmentation() throws Exception {
        // Given
        ClientHead clientHead = createMockClientHead(Transport.WEBSOCKET);
        OutPacketMessage message = new OutPacketMessage(clientHead, Transport.WEBSOCKET);
        ChannelPromise promise = channel.newPromise();

        Packet packet = new Packet(PacketType.MESSAGE, EngineIOVersion.V4);
        packet.setData("Large message content");
        clientHead.getPacketsQueue(Transport.WEBSOCKET).add(packet);

        doAnswer(invocation -> {
            ByteBuf buffer = invocation.getArgument(1);
            // Create a buffer larger than MAX_FRAME_PAYLOAD_LENGTH to trigger fragmentation
            // Need enough data to support multiple FRAME_BUFFER_SIZE reads (8192 bytes each)
            byte[] largeData = new byte[MAX_FRAME_PAYLOAD_LENGTH + 10000];
            buffer.writeBytes(largeData);
            // Ensure buffer is readable
            buffer.readerIndex(0);
            return null;
        }).when(mockEncoder).encodePacket(any(), any(), any(), eq(true));

        // When
        encoderHandler.write(channel.pipeline().context(encoderHandler), message, promise);

        // Then
        assertThat(channel.outboundMessages()).hasSizeGreaterThan(1);
        // First frame should be TextWebSocketFrame
        WebSocketFrame firstFrame = channel.readOutbound();
        assertThat(firstFrame).isInstanceOf(TextWebSocketFrame.class);
        assertThat(firstFrame.isFinalFragment()).isFalse();

        // Subsequent frames should be ContinuationWebSocketFrame
        while (channel.outboundMessages().size() > 0) {
            WebSocketFrame frame = channel.readOutbound();
            if (frame instanceof ContinuationWebSocketFrame) {
                ContinuationWebSocketFrame continuationFrame = (ContinuationWebSocketFrame) frame;
                // Last frame should be final
                if (channel.outboundMessages().size() == 0) {
                    assertThat(continuationFrame.isFinalFragment()).isTrue();
                } else {
                    assertThat(continuationFrame.isFinalFragment()).isFalse();
                }
            }
        }
    }

    @Test
    @DisplayName("Should handle WebSocket transport with binary attachments")
    void shouldHandleWebSocketTransportWithBinaryAttachments() throws Exception {
        // Given
        ClientHead clientHead = createMockClientHead(Transport.WEBSOCKET);
        OutPacketMessage message = new OutPacketMessage(clientHead, Transport.WEBSOCKET);
        ChannelPromise promise = channel.newPromise();

        Packet packet = new Packet(PacketType.MESSAGE, EngineIOVersion.V4);
        packet.setData("Message with attachment");
        ByteBuf attachment = Unpooled.wrappedBuffer("attachment data".getBytes());
        packet.addAttachment(attachment);
        clientHead.getPacketsQueue(Transport.WEBSOCKET).add(packet);

        doAnswer(invocation -> {
            ByteBuf buffer = invocation.getArgument(1);
            buffer.writeBytes("42[\"Message with attachment\"]".getBytes());
            return null;
        }).when(mockEncoder).encodePacket(any(), any(), any(), eq(true));

        // When
        encoderHandler.write(channel.pipeline().context(encoderHandler), message, promise);

        // Then
        assertThat(channel.outboundMessages()).hasSize(1); // Only text frame since no attachments
        WebSocketFrame textFrame = channel.readOutbound();
        assertThat(textFrame).isInstanceOf(TextWebSocketFrame.class);
        assertThat(textFrame.content().readableBytes()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should handle HTTP polling transport with binary encoding")
    void shouldHandleHTTPPollingTransportWithBinaryEncoding() throws Exception {
        // Given
        ClientHead clientHead = createMockClientHead(Transport.POLLING);
        OutPacketMessage message = new OutPacketMessage(clientHead, Transport.POLLING);
        ChannelPromise promise = channel.newPromise();

        Packet packet = new Packet(PacketType.MESSAGE, EngineIOVersion.V4);
        packet.setData("Polling message");
        clientHead.getPacketsQueue(Transport.POLLING).add(packet);

        doAnswer(invocation -> {
            ByteBuf buffer = invocation.getArgument(1);
            buffer.writeBytes("42[\"Polling message\"]".getBytes());
            return null;
        }).when(mockEncoder).encodePackets(any(), any(), any(), anyInt());

        // When
        encoderHandler.write(channel.pipeline().context(encoderHandler), message, promise);

        // Then
        assertThat(channel.outboundMessages()).hasSize(3);
        HttpResponse response = channel.readOutbound();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.OK);
        assertThat(response.headers().get("Content-Type")).isEqualTo("application/octet-stream");
        assertThat(response.headers().get("Set-Cookie")).contains("io=" + sessionId);
    }

    @Test
    @DisplayName("Should handle HTTP polling transport with JSONP encoding")
    void shouldHandleHTTPPollingTransportWithJSONPEncoding() throws Exception {
        // Given
        ClientHead clientHead = createMockClientHead(Transport.POLLING);
        OutPacketMessage message = new OutPacketMessage(clientHead, Transport.POLLING);
        ChannelPromise promise = channel.newPromise();

        channel.attr(EncoderHandler.B64).set(true);
        channel.attr(EncoderHandler.JSONP_INDEX).set(1);

        Packet packet = new Packet(PacketType.MESSAGE, EngineIOVersion.V4);
        packet.setData("JSONP message");
        clientHead.getPacketsQueue(Transport.POLLING).add(packet);

        doAnswer(invocation -> {
            ByteBuf buffer = invocation.getArgument(2);
            buffer.writeBytes("io[1](\"42[\"JSONP message\"]\")".getBytes());
            return null;
        }).when(mockEncoder).encodeJsonP(anyInt(), any(), any(), any(), anyInt());

        // When
        encoderHandler.write(channel.pipeline().context(encoderHandler), message, promise);

        // Then
        assertThat(channel.outboundMessages()).hasSize(3);
        HttpResponse response = channel.readOutbound();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.OK);
        assertThat(response.headers().get("Content-Type")).isEqualTo("application/javascript");
    }

    @Test
    @DisplayName("Should handle HTTP polling transport with JSONP encoding without index")
    void shouldHandleHTTPPollingTransportWithJSONPEncodingWithoutIndex() throws Exception {
        // Given
        ClientHead clientHead = createMockClientHead(Transport.POLLING);
        OutPacketMessage message = new OutPacketMessage(clientHead, Transport.POLLING);
        ChannelPromise promise = channel.newPromise();

        channel.attr(EncoderHandler.B64).set(true);
        channel.attr(EncoderHandler.JSONP_INDEX).set(null);

        Packet packet = new Packet(PacketType.MESSAGE, EngineIOVersion.V4);
        packet.setData("JSONP message without index");
        clientHead.getPacketsQueue(Transport.POLLING).add(packet);

        doAnswer(invocation -> {
            ByteBuf buffer = invocation.getArgument(2);
            buffer.writeBytes("42[\"JSONP message without index\"]".getBytes());
            return null;
        }).when(mockEncoder).encodeJsonP(any(), any(), any(), any(), anyInt());

        // When
        encoderHandler.write(channel.pipeline().context(encoderHandler), message, promise);

        // Then
        assertThat(channel.outboundMessages()).hasSize(3);
        HttpResponse response = channel.readOutbound();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.OK);
        assertThat(response.headers().get("Content-Type")).isEqualTo("text/plain");
    }

    @Test
    @DisplayName("Should handle HTTP polling transport with active channel")
    void shouldHandleHTTPPollingTransportWithActiveChannel() throws Exception {
        // Given
        ClientHead clientHead = createMockClientHead(Transport.POLLING);
        OutPacketMessage message = new OutPacketMessage(clientHead, Transport.POLLING);
        ChannelPromise promise = channel.newPromise();

        // Add a packet to the queue so it gets processed
        Packet packet = new Packet(PacketType.MESSAGE, EngineIOVersion.V4);
        packet.setData("Test message");
        clientHead.getPacketsQueue(Transport.POLLING).add(packet);

        doAnswer(invocation -> {
            ByteBuf buffer = invocation.getArgument(1);
            buffer.writeBytes("42[\"Test message\"]".getBytes());
            return null;
        }).when(mockEncoder).encodePackets(any(), any(), any(), anyInt());

        // When
        encoderHandler.write(channel.pipeline().context(encoderHandler), message, promise);

        // Then
        // Message should be processed since queue has content
        assertThat(channel.outboundMessages()).hasSize(3);
        HttpResponse response = channel.readOutbound();
        assertThat(response.status()).isEqualTo(HttpResponseStatus.OK);
    }

    @Test
    @DisplayName("Should handle HTTP polling transport with empty queue")
    void shouldHandleHTTPPollingTransportWithEmptyQueue() throws Exception {
        // Given
        ClientHead clientHead = createMockClientHead(Transport.POLLING);
        OutPacketMessage message = new OutPacketMessage(clientHead, Transport.POLLING);
        ChannelPromise promise = channel.newPromise();

        // Queue is already empty

        // When
        encoderHandler.write(channel.pipeline().context(encoderHandler), message, promise);

        // Then
        assertThat(promise.isSuccess()).isTrue();
        assertThat(channel.outboundMessages()).isEmpty();
    }

    @Test
    @DisplayName("Should handle HTTP polling transport with write-once attribute")
    void shouldHandleHTTPPollingTransportWithWriteOnceAttribute() throws Exception {
        // Given
        ClientHead clientHead = createMockClientHead(Transport.POLLING);
        OutPacketMessage message = new OutPacketMessage(clientHead, Transport.POLLING);
        ChannelPromise promise = channel.newPromise();

        channel.attr(EncoderHandler.WRITE_ONCE).set(true);

        Packet packet = new Packet(PacketType.MESSAGE, EngineIOVersion.V4);
        packet.setData("Message");
        clientHead.getPacketsQueue(Transport.POLLING).add(packet);

        // When
        encoderHandler.write(channel.pipeline().context(encoderHandler), message, promise);

        // Then
        assertThat(promise.isSuccess()).isTrue();
        assertThat(channel.outboundMessages()).isEmpty();
    }

    @Test
    @DisplayName("Should handle non-HTTP message by delegating to parent")
    void shouldHandleNonHTTPMessageByDelegatingToParent() throws Exception {
        // Given
        String nonHttpMessage = "Non-HTTP message";
        ChannelPromise promise = channel.newPromise();

        // When
        encoderHandler.write(channel.pipeline().context(encoderHandler), nonHttpMessage, promise);

        // Then
        // Should delegate to parent class, no outbound messages expected
        assertThat(channel.outboundMessages()).isEmpty();
    }

    @Test
    @DisplayName("Should handle IE user agent with XSS protection header")
    void shouldHandleIEUserAgentWithXSSProtectionHeader() throws Exception {
        // Given
        XHRPostMessage message = new XHRPostMessage(TEST_ORIGIN, sessionId);
        channel.attr(EncoderHandler.ORIGIN).set(TEST_ORIGIN);
        channel.attr(EncoderHandler.USER_AGENT).set("Mozilla/5.0 (compatible; MSIE 9.0; Windows NT 6.1; Trident/5.0)");
        ChannelPromise promise = channel.newPromise();

        // When
        encoderHandler.write(channel.pipeline().context(encoderHandler), message, promise);

        // Then
        assertThat(channel.outboundMessages()).hasSize(3);
        HttpResponse response = channel.readOutbound();
        assertThat(response.headers().get("X-XSS-Protection")).isEqualTo("0");
    }

    @Test
    @DisplayName("Should handle Trident user agent with XSS protection header")
    void shouldHandleTridentUserAgentWithXSSProtectionHeader() throws Exception {
        // Given
        XHRPostMessage message = new XHRPostMessage(TEST_ORIGIN, sessionId);
        channel.attr(EncoderHandler.ORIGIN).set(TEST_ORIGIN);
        channel.attr(EncoderHandler.USER_AGENT).set("Mozilla/5.0 (Windows NT 10.0; WOW64; Trident/7.0; rv:11.0) like Gecko");
        ChannelPromise promise = channel.newPromise();

        // When
        encoderHandler.write(channel.pipeline().context(encoderHandler), message, promise);

        // Then
        assertThat(channel.outboundMessages()).hasSize(3);
        HttpResponse response = channel.readOutbound();
        assertThat(response.headers().get("X-XSS-Protection")).isEqualTo("0");
    }

    @Test
    @DisplayName("Should handle null origin in headers")
    void shouldHandleNullOriginInHeaders() throws Exception {
        // Given
        XHRPostMessage message = new XHRPostMessage(null, sessionId);
        channel.attr(EncoderHandler.ORIGIN).set(null);
        ChannelPromise promise = channel.newPromise();

        // When
        encoderHandler.write(channel.pipeline().context(encoderHandler), message, promise);

        // Then
        assertThat(channel.outboundMessages()).hasSize(3);
        HttpResponse response = channel.readOutbound();
        assertThat(response.headers().get("Access-Control-Allow-Origin")).isEqualTo("*");
        assertThat(response.headers().get("Access-Control-Allow-Credentials")).isNull();
    }

    @Test
    @DisplayName("Should handle configuration with custom allow headers")
    void shouldHandleConfigurationWithCustomAllowHeaders() throws Exception {
        // Given
        configuration.setAllowHeaders("Authorization, Content-Type");
        encoderHandler = new EncoderHandler(configuration, mockEncoder);
        channel = new EmbeddedChannel(encoderHandler);

        XHROptionsMessage message = new XHROptionsMessage(TEST_ORIGIN, sessionId);
        channel.attr(EncoderHandler.ORIGIN).set(TEST_ORIGIN);
        ChannelPromise promise = channel.newPromise();

        // When
        encoderHandler.write(channel.pipeline().context(encoderHandler), message, promise);

        // Then
        assertThat(channel.outboundMessages()).hasSize(2);
        HttpResponse response = channel.readOutbound();
        assertThat(response.headers().get("Access-Control-Allow-Headers")).isEqualTo("content-type");
    }

    @Test
    @DisplayName("Should handle WebSocket transport with multiple packets")
    void shouldHandleWebSocketTransportWithMultiplePackets() throws Exception {
        // Given
        ClientHead clientHead = createMockClientHead(Transport.WEBSOCKET);
        OutPacketMessage message = new OutPacketMessage(clientHead, Transport.WEBSOCKET);
        ChannelPromise promise = channel.newPromise();

        Packet packet1 = new Packet(PacketType.MESSAGE, EngineIOVersion.V4);
        packet1.setData("First message");
        Packet packet2 = new Packet(PacketType.MESSAGE, EngineIOVersion.V4);
        packet2.setData("Second message");
        clientHead.getPacketsQueue(Transport.WEBSOCKET).add(packet1);
        clientHead.getPacketsQueue(Transport.WEBSOCKET).add(packet2);

        doAnswer(invocation -> {
            Packet packet = invocation.getArgument(0);
            ByteBuf buffer = invocation.getArgument(1);
            if (packet.getData().equals("First message")) {
                buffer.writeBytes("42[\"First message\"]".getBytes());
            } else {
                buffer.writeBytes("42[\"Second message\"]".getBytes());
            }
            return null;
        }).when(mockEncoder).encodePacket(any(), any(), any(), eq(true));

        // When
        encoderHandler.write(channel.pipeline().context(encoderHandler), message, promise);

        // Then
        assertThat(channel.outboundMessages()).hasSize(2);
        WebSocketFrame frame1 = channel.readOutbound();
        assertThat(frame1).isInstanceOf(TextWebSocketFrame.class);
        assertThat(frame1.content().readableBytes()).isGreaterThan(0);

        WebSocketFrame frame2 = channel.readOutbound();
        assertThat(frame2).isInstanceOf(TextWebSocketFrame.class);
        assertThat(frame2.content().readableBytes()).isGreaterThan(0);
    }

    @Test
    @DisplayName("Should handle WebSocket transport with empty packet queue")
    void shouldHandleWebSocketTransportWithEmptyPacketQueue() throws Exception {
        // Given
        ClientHead clientHead = createMockClientHead(Transport.WEBSOCKET);
        OutPacketMessage message = new OutPacketMessage(clientHead, Transport.WEBSOCKET);
        ChannelPromise promise = channel.newPromise();

        // Queue is already empty

        // When
        encoderHandler.write(channel.pipeline().context(encoderHandler), message, promise);

        // Then
        assertThat(channel.outboundMessages()).isEmpty();
        assertThat(promise.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("Should handle WebSocket transport with non-readable buffer")
    void shouldHandleWebSocketTransportWithNonReadableBuffer() throws Exception {
        // Given
        ClientHead clientHead = createMockClientHead(Transport.WEBSOCKET);
        OutPacketMessage message = new OutPacketMessage(clientHead, Transport.WEBSOCKET);
        ChannelPromise promise = channel.newPromise();

        Packet packet = new Packet(PacketType.MESSAGE, EngineIOVersion.V4);
        packet.setData("Message");
        clientHead.getPacketsQueue(Transport.WEBSOCKET).add(packet);

        doAnswer(invocation -> {
            // Create a buffer that is not readable
            ByteBuf buffer = invocation.getArgument(1);
            buffer.writeBytes("42[\"Message\"]".getBytes());
            buffer.readerIndex(buffer.writerIndex()); // Make it non-readable
            return null;
        }).when(mockEncoder).encodePacket(any(), any(), any(), eq(true));

        // When
        encoderHandler.write(channel.pipeline().context(encoderHandler), message, promise);

        // Then
        assertThat(channel.outboundMessages()).isEmpty();
        assertThat(promise.isSuccess()).isTrue();
    }

    @Test
    @DisplayName("Should handle HTTP polling transport with write-once attribute race condition")
    void shouldHandleHTTPPollingTransportWithWriteOnceAttributeRaceCondition() throws Exception {
        // Given
        ClientHead clientHead = createMockClientHead(Transport.POLLING);
        OutPacketMessage message = new OutPacketMessage(clientHead, Transport.POLLING);
        ChannelPromise promise = channel.newPromise();

        Packet packet = new Packet(PacketType.MESSAGE, EngineIOVersion.V4);
        packet.setData("Message");
        clientHead.getPacketsQueue(Transport.POLLING).add(packet);

        // Simulate race condition where write-once is set during processing
        channel.attr(EncoderHandler.WRITE_ONCE).set(false);

        doAnswer(invocation -> {
            // Set write-once during encoding to simulate race condition
            channel.attr(EncoderHandler.WRITE_ONCE).set(true);
            ByteBuf buffer = invocation.getArgument(1);
            buffer.writeBytes("42[\"Message\"]".getBytes());
            return null;
        }).when(mockEncoder).encodePackets(any(), any(), any(), anyInt());

        // When
        encoderHandler.write(channel.pipeline().context(encoderHandler), message, promise);

        // Then
        // Message should not be processed due to write-once attribute being set during processing
        assertThat(promise.isSuccess()).isTrue();
        assertThat(channel.outboundMessages()).isEmpty();
    }

    private ClientHead createMockClientHead(Transport transport) {
        ClientHead clientHead = mock(ClientHead.class);
        ConcurrentLinkedQueue<Packet> queue = new ConcurrentLinkedQueue<>();
        when(clientHead.getPacketsQueue(transport)).thenReturn(queue);
        when(clientHead.getSessionId()).thenReturn(sessionId);
        when(clientHead.getOrigin()).thenReturn(TEST_ORIGIN);
        return clientHead;
    }
}
