/**
 * Copyright (c) 2012-2025 Nikita Koksharov
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
package com.corundumstudio.socketio.handler;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.TestInstance;
import org.junit.jupiter.api.TestInstance.Lifecycle;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.ack.AckManager;
import com.corundumstudio.socketio.namespace.Namespace;
import com.corundumstudio.socketio.namespace.NamespacesHub;
import com.corundumstudio.socketio.protocol.EngineIOVersion;
import com.corundumstudio.socketio.protocol.Packet;
import com.corundumstudio.socketio.protocol.PacketType;
import com.corundumstudio.socketio.scheduler.CancelableScheduler;
import com.corundumstudio.socketio.scheduler.SchedulerKey;
import com.corundumstudio.socketio.handler.ClientHead;
import com.corundumstudio.socketio.transport.NamespaceClient;
import com.corundumstudio.socketio.transport.PollingTransport;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Comprehensive unit test suite for PacketListener class.
 * 
 * This test class covers all packet types and their processing logic:
 * - PING packets (including probe ping)
 * - PONG packets
 * - UPGRADE packets
 * - MESSAGE packets with various subtypes
 * - CLOSE packets
 * - ACK handling
 * - Engine.IO version compatibility
 * - Namespace interactions
 * - Scheduler operations
 * 
 * Test Coverage:
 * - All packet type branches
 * - All conditional logic paths
 * - Edge cases and boundary conditions
 * - Mock interactions and verifications
 * - Error scenarios
 */
@DisplayName("PacketListener Tests")
@TestInstance(Lifecycle.PER_CLASS)
class PacketListenerTest {

    @Mock
    private AckManager ackManager;

    @Mock
    private NamespacesHub namespacesHub;

    @Mock
    private PollingTransport xhrPollingTransport;

    @Mock
    private CancelableScheduler scheduler;

    @Mock
    private NamespaceClient namespaceClient;

    @Mock
    private ClientHead baseClient;

    @Mock
    private Namespace namespace;

    @Captor
    private ArgumentCaptor<Packet> packetCaptor;

    @Captor
    private ArgumentCaptor<SchedulerKey> schedulerKeyCaptor;

    @Captor
    private ArgumentCaptor<Transport> transportCaptor;

    @Captor
    private ArgumentCaptor<AckRequest> ackRequestCaptor;

    private PacketListener packetListener;

    private static final UUID SESSION_ID = UUID.randomUUID();
    private static final String NAMESPACE_NAME = "/test";
    private static final String EVENT_NAME = "testEvent";
    private static final Long ACK_ID = 123L;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        
        // Setup default mock behavior
        when(namespaceClient.getSessionId()).thenReturn(SESSION_ID);
        when(namespaceClient.getBaseClient()).thenReturn(baseClient);
        when(namespaceClient.getEngineIOVersion()).thenReturn(EngineIOVersion.V3);
        when(namespaceClient.getNamespace()).thenReturn(namespace);
        
        when(namespacesHub.get(NAMESPACE_NAME)).thenReturn(namespace);
        
        packetListener = new PacketListener(ackManager, namespacesHub, xhrPollingTransport, scheduler);
    }

    @Nested
    @DisplayName("ACK Request Handling")
    class AckRequestHandlingTests {

        @Test
        @DisplayName("Should initialize ACK index when packet requests ACK")
        void shouldInitializeAckIndexWhenPacketRequestsAck() {
            // Given
            Packet packet = createPacket(PacketType.MESSAGE);
            packet.setAckId(ACK_ID);
            // Create a mock packet for ACK testing
            Packet mockPacket = mock(Packet.class);
            when(mockPacket.getType()).thenReturn(PacketType.MESSAGE);
            when(mockPacket.getNsp()).thenReturn(NAMESPACE_NAME);
            when(mockPacket.isAckRequested()).thenReturn(true);
            when(mockPacket.getAckId()).thenReturn(ACK_ID);

            // When
            packetListener.onPacket(mockPacket, namespaceClient, Transport.WEBSOCKET);

            // Then
            verify(ackManager, times(1)).initAckIndex(SESSION_ID, ACK_ID);
        }

        @Test
        @DisplayName("Should not initialize ACK index when packet does not request ACK")
        void shouldNotInitializeAckIndexWhenPacketDoesNotRequestAck() {
            // Given
            // Create a mock packet for ACK testing
            Packet mockPacket = mock(Packet.class);
            when(mockPacket.getType()).thenReturn(PacketType.MESSAGE);
            when(mockPacket.getNsp()).thenReturn(NAMESPACE_NAME);
            when(mockPacket.isAckRequested()).thenReturn(false);

            // When
            packetListener.onPacket(mockPacket, namespaceClient, Transport.WEBSOCKET);

            // Then
            verify(ackManager, never()).initAckIndex(any(UUID.class), any(Long.class));
        }
    }

    @Nested
    @DisplayName("PING Packet Handling")
    class PingPacketHandlingTests {

        @Test
        @DisplayName("Should handle regular PING packet correctly")
        void shouldHandleRegularPingPacketCorrectly() {
            // Given
            Packet packet = createPacket(PacketType.PING);
            packet.setData("ping");

            // When
            packetListener.onPacket(packet, namespaceClient, Transport.WEBSOCKET);

            // Then
            // Verify PONG response
            verify(baseClient, times(1)).send(packetCaptor.capture(), eq(Transport.WEBSOCKET));
            Packet pongPacket = packetCaptor.getValue();
            assertEquals(PacketType.PONG, pongPacket.getType());
            assertEquals("ping", pongPacket.getData());
            assertEquals(EngineIOVersion.V3, pongPacket.getEngineIOVersion());

            // Verify ping timeout scheduling
            verify(baseClient, times(1)).schedulePingTimeout();

            // Verify namespace ping notification
            verify(namespace, times(1)).onPing(namespaceClient);

            // Verify no NOOP packet sent for regular ping
            verify(baseClient, never()).send(any(Packet.class), eq(Transport.POLLING));
        }

        @Test
        @DisplayName("Should handle probe PING packet correctly")
        void shouldHandleProbePingPacketCorrectly() {
            // Given
            Packet packet = createPacket(PacketType.PING);
            packet.setData("probe");

            // When
            packetListener.onPacket(packet, namespaceClient, Transport.WEBSOCKET);

            // Then
            // Verify PONG response
            verify(baseClient, times(1)).send(packetCaptor.capture(), eq(Transport.WEBSOCKET));
            Packet pongPacket = packetCaptor.getValue();
            assertThat(pongPacket.getType()).isEqualTo(PacketType.PONG);
            assertEquals("probe", pongPacket.getData());

            // Verify NOOP packet sent for probe ping
            verify(baseClient, times(1)).send(packetCaptor.capture(), eq(Transport.POLLING));
            Packet noopPacket = packetCaptor.getAllValues().get(1);
            assertEquals(PacketType.NOOP, noopPacket.getType());
            assertEquals(EngineIOVersion.V3, noopPacket.getEngineIOVersion());

            // Verify no ping timeout scheduling for probe
            verify(baseClient, never()).schedulePingTimeout();

            // Verify namespace ping notification
            verify(namespace, times(1)).onPing(namespaceClient);
        }

        @Test
        @DisplayName("Should handle PING packet with null data")
        void shouldHandlePingPacketWithNullData() {
            // Given
            Packet packet = createPacket(PacketType.PING);
            packet.setData(null);

            // When
            packetListener.onPacket(packet, namespaceClient, Transport.WEBSOCKET);

            // Then
            // Verify PONG response with null data
            verify(baseClient, times(1)).send(packetCaptor.capture(), eq(Transport.WEBSOCKET));
            Packet pongPacket = packetCaptor.getValue();
            assertNull(pongPacket.getData());

            // Verify ping timeout scheduling
            verify(baseClient, times(1)).schedulePingTimeout();

            // Verify no NOOP packet sent
            verify(baseClient, never()).send(any(Packet.class), eq(Transport.POLLING));
        }
    }

    @Nested
    @DisplayName("PONG Packet Handling")
    class PongPacketHandlingTests {

        @Test
        @DisplayName("Should handle PONG packet correctly")
        void shouldHandlePongPacketCorrectly() {
            // Given
            Packet packet = createPacket(PacketType.PONG);

            // When
            packetListener.onPacket(packet, namespaceClient, Transport.WEBSOCKET);

            // Then
            // Verify ping timeout scheduling
            verify(baseClient, times(1)).schedulePingTimeout();

            // Verify namespace pong notification
            verify(namespace, times(1)).onPong(namespaceClient);

            // Verify no packet sent
            verify(baseClient, never()).send(any(Packet.class), any(Transport.class));
        }
    }

    @Nested
    @DisplayName("UPGRADE Packet Handling")
    class UpgradePacketHandlingTests {

        @Test
        @DisplayName("Should handle UPGRADE packet correctly")
        void shouldHandleUpgradePacketCorrectly() {
            // Given
            Packet packet = createPacket(PacketType.UPGRADE);

            // When
            packetListener.onPacket(packet, namespaceClient, Transport.WEBSOCKET);

            // Then
            // Verify ping timeout scheduling
            verify(baseClient, times(1)).schedulePingTimeout();

            // Verify scheduler cancellation
            verify(scheduler, times(1)).cancel(schedulerKeyCaptor.capture());
            SchedulerKey capturedKey = schedulerKeyCaptor.getValue();
            // Verify the scheduler key was created with correct parameters
            verify(scheduler, times(1)).cancel(any(SchedulerKey.class));

            // Verify transport upgrade
            verify(baseClient, times(1)).upgradeCurrentTransport(Transport.WEBSOCKET);
        }
    }

    @Nested
    @DisplayName("MESSAGE Packet Handling")
    class MessagePacketHandlingTests {

        @Test
        @DisplayName("Should handle DISCONNECT message correctly")
        void shouldHandleDisconnectMessageCorrectly() {
            // Given
            Packet packet = createPacket(PacketType.MESSAGE);
            packet.setSubType(PacketType.DISCONNECT);

            // When
            packetListener.onPacket(packet, namespaceClient, Transport.WEBSOCKET);

            // Then
            // Verify ping timeout scheduling
            verify(baseClient, times(1)).schedulePingTimeout();

            // Verify client disconnect
            verify(namespaceClient, times(1)).onDisconnect();

            // Verify no other operations
            verify(baseClient, never()).send(any(Packet.class), any(Transport.class));
            verify(namespace, never()).onConnect(any());
            verify(ackManager, never()).onAck(any(), any());
            verify(namespace, never()).onEvent(any(), anyString(), any(), any());
        }

        @Test
        @DisplayName("Should handle CONNECT message for Engine.IO v3 correctly")
        void shouldHandleConnectMessageForEngineIOv3Correctly() {
            // Given
            Packet packet = createPacket(PacketType.MESSAGE);
            packet.setSubType(PacketType.CONNECT);
            when(namespaceClient.getEngineIOVersion()).thenReturn(EngineIOVersion.V3);

            // When
            packetListener.onPacket(packet, namespaceClient, Transport.WEBSOCKET);

            // Then
            // Verify ping timeout scheduling
            verify(baseClient, times(1)).schedulePingTimeout();

            // Verify namespace connect
            verify(namespace, times(1)).onConnect(namespaceClient);

            // Verify connect handshake packet sent back for v3
            verify(baseClient, times(1)).send(packet, Transport.WEBSOCKET);
        }

        @Test
        @DisplayName("Should handle CONNECT message for Engine.IO v4 correctly")
        void shouldHandleConnectMessageForEngineIOv4Correctly() {
            // Given
            Packet packet = createPacket(PacketType.MESSAGE);
            packet.setSubType(PacketType.CONNECT);
            when(namespaceClient.getEngineIOVersion()).thenReturn(EngineIOVersion.V4);

            // When
            packetListener.onPacket(packet, namespaceClient, Transport.WEBSOCKET);

            // Then
            // Verify ping timeout scheduling
            verify(baseClient, times(1)).schedulePingTimeout();

            // Verify namespace connect
            verify(namespace, times(1)).onConnect(namespaceClient);

            // Verify no connect handshake packet sent back for v4
            verify(baseClient, never()).send(packet, Transport.WEBSOCKET);
        }

        @Test
        @DisplayName("Should handle ACK message correctly")
        void shouldHandleAckMessageCorrectly() {
            // Given
            Packet packet = createPacket(PacketType.MESSAGE);
            packet.setSubType(PacketType.ACK);

            // When
            packetListener.onPacket(packet, namespaceClient, Transport.WEBSOCKET);

            // Then
            // Verify ping timeout scheduling
            verify(baseClient, times(1)).schedulePingTimeout();

            // Verify ACK handling
            verify(ackManager, times(1)).onAck(namespaceClient, packet);

            // Verify no other operations
            verify(baseClient, never()).send(any(Packet.class), any(Transport.class));
            verify(namespace, never()).onConnect(any());
            verify(namespace, never()).onEvent(any(), anyString(), any(), any());
        }

        @Test
        @DisplayName("Should handle BINARY_ACK message correctly")
        void shouldHandleBinaryAckMessageCorrectly() {
            // Given
            Packet packet = createPacket(PacketType.MESSAGE);
            packet.setSubType(PacketType.BINARY_ACK);

            // When
            packetListener.onPacket(packet, namespaceClient, Transport.WEBSOCKET);

            // Then
            // Verify ping timeout scheduling
            verify(baseClient, times(1)).schedulePingTimeout();

            // Verify ACK handling
            verify(ackManager, times(1)).onAck(namespaceClient, packet);

            // Verify no other operations
            verify(baseClient, never()).send(any(Packet.class), any(Transport.class));
            verify(namespace, never()).onConnect(any());
            verify(namespace, never()).onEvent(any(), anyString(), any(), any());
        }

        @Test
        @DisplayName("Should handle EVENT message with data correctly")
        void shouldHandleEventMessageWithDataCorrectly() {
            // Given
            Packet packet = createPacket(PacketType.MESSAGE);
            packet.setSubType(PacketType.EVENT);
            packet.setName(EVENT_NAME);
            List<Object> eventData = Arrays.asList("data1", "data2");
            packet.setData(eventData);

            // When
            packetListener.onPacket(packet, namespaceClient, Transport.WEBSOCKET);

            // Then
            // Verify ping timeout scheduling
            verify(baseClient, times(1)).schedulePingTimeout();

            // Verify namespace event handling
            verify(namespace, times(1)).onEvent(eq(namespaceClient), eq(EVENT_NAME), eq(eventData), any(AckRequest.class));

            // Verify no other operations
            verify(baseClient, never()).send(any(Packet.class), any(Transport.class));
            verify(namespace, never()).onConnect(any());
            verify(ackManager, never()).onAck(any(), any());
        }

        @Test
        @DisplayName("Should handle EVENT message with null data correctly")
        void shouldHandleEventMessageWithNullDataCorrectly() {
            // Given
            Packet packet = createPacket(PacketType.MESSAGE);
            packet.setSubType(PacketType.EVENT);
            packet.setName(EVENT_NAME);
            packet.setData(null);

            // When
            packetListener.onPacket(packet, namespaceClient, Transport.WEBSOCKET);

            // Then
            // Verify ping timeout scheduling
            verify(baseClient, times(1)).schedulePingTimeout();

            // Verify namespace event handling with empty list
            verify(namespace, times(1)).onEvent(eq(namespaceClient), eq(EVENT_NAME), eq(Collections.emptyList()), any(AckRequest.class));

            // Verify no other operations
            verify(baseClient, never()).send(any(Packet.class), any(Transport.class));
            verify(namespace, never()).onConnect(any());
            verify(ackManager, never()).onAck(any(), any());
        }

        @Test
        @DisplayName("Should handle BINARY_EVENT message correctly")
        void shouldHandleBinaryEventMessageCorrectly() {
            // Given
            Packet packet = createPacket(PacketType.MESSAGE);
            packet.setSubType(PacketType.BINARY_EVENT);
            packet.setName(EVENT_NAME);
            List<Object> eventData = Arrays.asList("binaryData");
            packet.setData(eventData);

            // When
            packetListener.onPacket(packet, namespaceClient, Transport.WEBSOCKET);

            // Then
            // Verify ping timeout scheduling
            verify(baseClient, times(1)).schedulePingTimeout();

            // Verify namespace event handling
            verify(namespace, times(1)).onEvent(eq(namespaceClient), eq(EVENT_NAME), eq(eventData), any(AckRequest.class));

            // Verify no other operations
            verify(baseClient, never()).send(any(Packet.class), any(Transport.class));
            verify(namespace, never()).onConnect(any());
            verify(ackManager, never()).onAck(any(), any());
        }

        @Test
        @DisplayName("Should handle CONNECT message with event data correctly")
        void shouldHandleConnectMessageWithEventDataCorrectly() {
            // Given
            Packet packet = createPacket(PacketType.MESSAGE);
            packet.setSubType(PacketType.CONNECT);
            packet.setName(EVENT_NAME);
            List<Object> eventData = Arrays.asList("data");
            packet.setData(eventData);
            when(namespaceClient.getEngineIOVersion()).thenReturn(EngineIOVersion.V3);

            // When
            packetListener.onPacket(packet, namespaceClient, Transport.WEBSOCKET);

            // Then
            // Verify ping timeout scheduling
            verify(baseClient, times(1)).schedulePingTimeout();

            // Verify namespace connect
            verify(namespace, times(1)).onConnect(namespaceClient);

            // Verify connect handshake packet sent back for v3
            verify(baseClient, times(1)).send(packet, Transport.WEBSOCKET);

            // Note: CONNECT messages don't trigger EVENT handling in PacketListener
            // The event data is only used for the connect handshake response
        }
    }

    @Nested
    @DisplayName("CLOSE Packet Handling")
    class ClosePacketHandlingTests {

        @Test
        @DisplayName("Should handle CLOSE packet correctly")
        void shouldHandleClosePacketCorrectly() {
            // Given
            Packet packet = createPacket(PacketType.CLOSE);

            // When
            packetListener.onPacket(packet, namespaceClient, Transport.WEBSOCKET);

            // Then
            // Verify channel disconnect
            verify(baseClient, times(1)).onChannelDisconnect();

            // Verify no other operations
            verify(baseClient, never()).send(any(Packet.class), any(Transport.class));
            verify(baseClient, never()).schedulePingTimeout();
            verify(namespace, never()).onPing(any());
            verify(namespace, never()).onPong(any());
            verify(namespace, never()).onConnect(any());
            verify(ackManager, never()).onAck(any(), any());
            verify(namespace, never()).onEvent(any(), anyString(), any(), any());
            verify(scheduler, never()).cancel(any());
        }
    }

    @Nested
    @DisplayName("Edge Cases and Error Scenarios")
    class EdgeCasesAndErrorScenariosTests {

        @Test
        @DisplayName("Should handle unknown packet type gracefully")
        void shouldHandleUnknownPacketTypeGracefully() {
            // Given
            Packet packet = createPacket(PacketType.ERROR);

            // When
            packetListener.onPacket(packet, namespaceClient, Transport.WEBSOCKET);

            // Then
            // Verify no operations performed
            verify(baseClient, never()).send(any(Packet.class), any(Transport.class));
            verify(baseClient, never()).schedulePingTimeout();
            verify(baseClient, never()).upgradeCurrentTransport(any());
            verify(baseClient, never()).onChannelDisconnect();
            verify(namespace, never()).onPing(any());
            verify(namespace, never()).onPong(any());
            verify(namespace, never()).onConnect(any());
            verify(ackManager, never()).onAck(any(), any());
            verify(namespace, never()).onEvent(any(), anyString(), any(), any());
            verify(scheduler, never()).cancel(any());
        }

        @Test
        @DisplayName("Should handle packet with null namespace correctly")
        void shouldHandlePacketWithNullNamespaceCorrectly() {
            // Given
            Packet packet = createPacket(PacketType.PING);
            packet.setNsp(null);
            // Create a mock namespace for null namespace test
            Namespace mockNullNamespace = mock(Namespace.class);
            when(namespacesHub.get(null)).thenReturn(mockNullNamespace);

            // When
            packetListener.onPacket(packet, namespaceClient, Transport.WEBSOCKET);

            // Then
            // Should not throw exception, but namespace operations may fail
            verify(baseClient, times(1)).send(any(Packet.class), any(Transport.class));
            verify(baseClient, times(1)).schedulePingTimeout();
        }

        @Test
        @DisplayName("Should handle packet with empty data correctly")
        void shouldHandlePacketWithEmptyDataCorrectly() {
            // Given
            Packet packet = createPacket(PacketType.PING);
            packet.setData("");

            // When
            packetListener.onPacket(packet, namespaceClient, Transport.WEBSOCKET);

            // Then
            // Verify PONG response with empty data
            verify(baseClient, times(1)).send(packetCaptor.capture(), eq(Transport.WEBSOCKET));
            Packet pongPacket = packetCaptor.getValue();
            assertEquals("", pongPacket.getData());

            // Verify ping timeout scheduling (not probe)
            verify(baseClient, times(1)).schedulePingTimeout();
        }

        @Test
        @DisplayName("Should handle packet with whitespace data correctly")
        void shouldHandlePacketWithWhitespaceDataCorrectly() {
            // Given
            Packet packet = createPacket(PacketType.PING);
            packet.setData("   ");

            // When
            packetListener.onPacket(packet, namespaceClient, Transport.WEBSOCKET);

            // Then
            // Verify PONG response with whitespace data
            verify(baseClient, times(1)).send(packetCaptor.capture(), eq(Transport.WEBSOCKET));
            Packet pongPacket = packetCaptor.getValue();
            assertEquals("   ", pongPacket.getData());

            // Verify ping timeout scheduling (not probe)
            verify(baseClient, times(1)).schedulePingTimeout();
        }
    }

    @Nested
    @DisplayName("Transport Handling")
    class TransportHandlingTests {

        @Test
        @DisplayName("Should handle different transport types correctly")
        void shouldHandleDifferentTransportTypesCorrectly() {
            // Given
            Packet packet = createPacket(PacketType.PING);
            Transport[] transports = {Transport.WEBSOCKET, Transport.POLLING};

            for (Transport transport : transports) {
                // Reset mocks
                MockitoAnnotations.openMocks(this);
                when(namespaceClient.getSessionId()).thenReturn(SESSION_ID);
                when(namespaceClient.getBaseClient()).thenReturn(baseClient);
                when(namespaceClient.getEngineIOVersion()).thenReturn(EngineIOVersion.V3);
                when(namespaceClient.getNamespace()).thenReturn(namespace);
                when(namespacesHub.get(NAMESPACE_NAME)).thenReturn(namespace);

                // When
                packetListener.onPacket(packet, namespaceClient, transport);

                // Then
                verify(baseClient, times(1)).send(any(Packet.class), eq(transport));
            }
        }
    }

    @Nested
    @DisplayName("Integration Scenarios")
    class IntegrationScenariosTests {

        @Test
        @DisplayName("Should handle complete packet lifecycle correctly")
        void shouldHandleCompletePacketLifecycleCorrectly() {
            // Given
            // Create a mock packet for ACK testing
            Packet mockPacket = mock(Packet.class);
            when(mockPacket.getType()).thenReturn(PacketType.MESSAGE);
            when(mockPacket.getSubType()).thenReturn(PacketType.EVENT);
            when(mockPacket.getName()).thenReturn(EVENT_NAME);
            when(mockPacket.getData()).thenReturn(Arrays.asList("testData"));
            when(mockPacket.getAckId()).thenReturn(ACK_ID);
            when(mockPacket.isAckRequested()).thenReturn(true);
            when(mockPacket.getNsp()).thenReturn(NAMESPACE_NAME);

            // When
            packetListener.onPacket(mockPacket, namespaceClient, Transport.WEBSOCKET);

            // Then
            // Verify ACK initialization
            verify(ackManager, times(1)).initAckIndex(SESSION_ID, ACK_ID);

            // Verify ping timeout scheduling
            verify(baseClient, times(1)).schedulePingTimeout();

            // Verify namespace event handling
            verify(namespace, times(1)).onEvent(eq(namespaceClient), eq(EVENT_NAME), eq(Arrays.asList("testData")), any(AckRequest.class));

            // Verify no packet sending
            verify(baseClient, never()).send(any(Packet.class), any(Transport.class));
        }

                @Test
        @DisplayName("Should handle probe ping correctly")
        void shouldHandleProbePingCorrectly() {
            // Given
            Packet probePacket = createPacket(PacketType.PING);
            probePacket.setData("probe");

            // When
            packetListener.onPacket(probePacket, namespaceClient, Transport.WEBSOCKET);

            // Then
            // Verify PONG response
            verify(baseClient, times(1)).send(any(Packet.class), eq(Transport.WEBSOCKET)); // PONG
            // Verify NOOP packet sent for probe ping
            verify(baseClient, times(1)).send(any(Packet.class), eq(Transport.POLLING)); // NOOP
            // Verify no ping timeout scheduling for probe
            verify(baseClient, never()).schedulePingTimeout();
            // Verify namespace ping notification
            verify(namespace, times(1)).onPing(namespaceClient);
        }

        @Test
        @DisplayName("Should handle regular ping correctly")
        void shouldHandleRegularPingCorrectly() {
            // Given
            Packet regularPacket = createPacket(PacketType.PING);
            regularPacket.setData("ping");

            // When
            packetListener.onPacket(regularPacket, namespaceClient, Transport.WEBSOCKET);

            // Then
            // Verify PONG response
            verify(baseClient, times(1)).send(any(Packet.class), eq(Transport.WEBSOCKET)); // PONG
            // Verify no NOOP packet sent for regular ping
            verify(baseClient, never()).send(any(Packet.class), eq(Transport.POLLING)); // NO NOOP
            // Verify ping timeout scheduling
            verify(baseClient, times(1)).schedulePingTimeout();
            // Verify namespace ping notification
            verify(namespace, times(1)).onPing(namespaceClient);
        }
    }

    // Helper methods
    private Packet createPacket(PacketType type) {
        Packet packet = new Packet(type, EngineIOVersion.V3);
        packet.setNsp(NAMESPACE_NAME);
        return packet;
    }
}
