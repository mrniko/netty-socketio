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
package com.corundumstudio.socketio.test.spring.boot.starter.annotation;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.Objects;
import java.util.Vector;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.annotation.OnConnect;
import com.corundumstudio.socketio.annotation.OnDisconnect;
import com.corundumstudio.socketio.annotation.OnEvent;
import com.corundumstudio.socketio.test.spring.boot.starter.BaseSpringApplicationTest;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import io.socket.client.Ack;
import io.socket.client.IO;
import io.socket.client.Socket;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@Import(AnnotationHandleTest.TestConfig.class)
public class AnnotationHandleTest extends BaseSpringApplicationTest {
    private static final Logger log = LoggerFactory.getLogger(AnnotationHandleTest.class);
    private static final int PORT = 9091;

    @DynamicPropertySource
    public static void setProperties(DynamicPropertyRegistry registry) {
        registry.add("netty-socket-io.port", () -> PORT);
    }

    public static class TestConnectController {
        private final AtomicInteger counter = new AtomicInteger(0);
        private final Vector<Object> params = new Vector<>();

        @OnConnect
        public void onConnectWithSocketIOClient(SocketIOClient socketIOClient) {
            log.info("onConnectWithSocketIOClient: {}", socketIOClient.getSessionId());
            counter.incrementAndGet();
            params.add(socketIOClient);
        }

        public void reset() {
            counter.set(0);
            params.clear();
        }
    }

    public static class TestDisconnectController {
        private final AtomicInteger counter = new AtomicInteger(0);
        private final Vector<Object> params = new Vector<>();

        @OnDisconnect
        public void onDisconnectWithSocketIOClient(SocketIOClient socketIOClient) {
            log.info("onDisconnectWithSocketIOClient: {}", socketIOClient.getSessionId());
            counter.incrementAndGet();
            params.add(socketIOClient);
        }

        public void reset() {
            counter.set(0);
            params.clear();
        }
    }

    public static class TestOnEventController {
        private final AtomicInteger counter = new AtomicInteger(0);
        private final Vector<Object> params = new Vector<>();
        private static final String EVENT_NAME_1 = "event1";
        private static final String EVENT_NAME_2 = "event2";
        private static final String EVENT_NAME_3 = "event3";
        private static final String EVENT_NAME_4 = "event4";


        @OnEvent(EVENT_NAME_1)
        public void onEvent1(SocketIOClient socketIOClient) {
            log.info("onEvent1: {}", socketIOClient.getSessionId());
            counter.incrementAndGet();
            params.add(socketIOClient);
        }

        @OnEvent(EVENT_NAME_2)
        public void onEvent2(AckRequest ackRequest) {
            log.info("onEvent2: {}", ackRequest.isAckRequested());
            counter.incrementAndGet();
            params.add(ackRequest);
            ackRequest.sendAckData(TestData.TEST_ACK_DATA);
        }

        @OnEvent(EVENT_NAME_3)
        public void onEvent3(SocketIOClient socketIOClient, AckRequest ackRequest) {
            log.info("onEvent3: {}, {}", socketIOClient.getSessionId(), ackRequest.isAckRequested());
            counter.incrementAndGet();
            params.add(socketIOClient);
            params.add(ackRequest);
            ackRequest.sendAckData(TestData.TEST_ACK_DATA);
        }

        @OnEvent(EVENT_NAME_4)
        public void onEvent4(AckRequest ackRequest, SocketIOClient socketIOClient, String data) {
            log.info("onEvent4: {}, {}, {}", socketIOClient.getSessionId(), ackRequest.isAckRequested(), data);
            counter.incrementAndGet();
            params.add(ackRequest);
            params.add(socketIOClient);
            params.add(data);
            ackRequest.sendAckData(TestData.TEST_ACK_DATA);
        }

        public void reset() {
            counter.set(0);
            params.clear();
        }
    }

    public static class TestConfig {
        @Bean
        public TestConnectController testConnectController() {
            return new TestConnectController();
        }

        @Bean
        public TestDisconnectController testDisconnectController() {
            return new TestDisconnectController();
        }

        @Bean
        public TestOnEventController testOnEventController() {
            return new TestOnEventController();
        }
    }

    public static class TestData {
        public static final TestData TEST_REQ_DATA = new TestData(
                "test", 18, 99.9,
                Timestamp.valueOf(LocalDateTime.of(2024, 6, 1, 12, 0, 0))
        );
        public static final TestData TEST_ACK_DATA = new TestData(
                "example", 25, 88.8,
                Timestamp.valueOf(LocalDateTime.of(2024, 6, 2, 15, 30, 0))
        );

        private String name;
        private int age;
        private double score;
        private Timestamp timestamp;

        public TestData() {
        }

        public TestData(String name, int age, double score, Timestamp timestamp) {
            this.name = name;
            this.age = age;
            this.score = score;
            this.timestamp = timestamp;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public int getAge() {
            return age;
        }

        public void setAge(int age) {
            this.age = age;
        }

        public double getScore() {
            return score;
        }

        public void setScore(double score) {
            this.score = score;
        }

        public Timestamp getTimestamp() {
            return timestamp;
        }

        public void setTimestamp(Timestamp timestamp) {
            this.timestamp = timestamp;
        }

        @Override
        public boolean equals(Object o) {
            if (!(o instanceof TestData)) return false;
            TestData testData = (TestData) o;
            return getAge() == testData.getAge()
                    && Double.compare(getScore(), testData.getScore()) == 0
                    && Objects.equals(getName(), testData.getName())
                    && Objects.equals(getTimestamp(), testData.getTimestamp());
        }

        @Override
        public int hashCode() {
            return Objects.hash(getName(), getAge(), getScore(), getTimestamp());
        }
    }

    private Socket socket;

    @BeforeEach
    public void setup() throws Exception {
        testConnectController.reset();
        testDisconnectController.reset();
        testOnEventController.reset();
        socket = IO.socket(
                String.format("http://localhost:%d", PORT),
                IO.Options.builder().setForceNew(true).build()
        );
        socket.connect();
        // wait for connection
        await().atMost(5, TimeUnit.SECONDS).until(() -> socket.connected());
    }

    @AfterEach
    public void tearDown() throws Exception {
        if (socket != null && socket.connected()) {
            socket.disconnect();
        }
    }

    @Autowired
    private TestConnectController testConnectController;
    @Autowired
    private TestDisconnectController testDisconnectController;
    @Autowired
    private TestOnEventController testOnEventController;

    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    public void testOnConnect() throws Exception {
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> testConnectController.counter.get() == 1
                        && testConnectController.params.size() == 1);
        assertEquals(1, testConnectController.counter.get(),
                "onConnect methods should be called");
        assertEquals(1, testConnectController.params.size(),
                "onConnect method should have SocketIOClient parameter");
        assertTrue(SocketIOClient.class.isAssignableFrom(testConnectController.params.get(0).getClass()),
                "Parameter should be of type SocketIOClient");
    }

    @Test
    public void testOnDisconnect() throws Exception {
        socket.disconnect();
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> testDisconnectController.counter.get() == 1
                        && testDisconnectController.params.size() == 1);
        assertEquals(1, testDisconnectController.counter.get(),
                "onDisconnect methods should be called");
        assertEquals(1, testDisconnectController.params.size(),
                "onDisconnect method should have SocketIOClient parameter");
        assertTrue(SocketIOClient.class.isAssignableFrom(testDisconnectController.params.get(0).getClass()),
                "Parameter should be of type SocketIOClient");
    }

    @Test
    public void testOnEvent1() throws Exception {
        socket.emit(TestOnEventController.EVENT_NAME_1);
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> testOnEventController.counter.get() == 1
                        && testOnEventController.params.size() == 1);
        assertEquals(1, testOnEventController.counter.get(),
                "onEvent1 methods should be called");
        assertEquals(1, testOnEventController.params.size(),
                "onEvent1 method should have SocketIOClient parameter");
        assertTrue(SocketIOClient.class.isAssignableFrom(testOnEventController.params.get(0).getClass()),
                "Parameter should be of type SocketIOClient");
    }

    @Test
    public void testOnEvent2() throws Exception {
        AtomicReference<TestData> ackDataRef = new AtomicReference<>();
        socket.emit(TestOnEventController.EVENT_NAME_2, null, new Ack() {
            @Override
            public void call(Object... objects) {
                TestData testData = null;
                try {
                    testData = objectMapper.readValue(objects[0].toString(), TestData.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                ackDataRef.set(testData);
            }
        });
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> testOnEventController.counter.get() == 1
                        && testOnEventController.params.size() == 1
                        && ackDataRef.get() != null);
        assertEquals(1, testOnEventController.counter.get(),
                "onEvent2 methods should be called");
        assertEquals(1, testOnEventController.params.size(),
                "onEvent2 method should have AckRequest parameter");
        assertEquals(TestData.TEST_ACK_DATA, ackDataRef.get(), "Ack data should match");
    }

    @Test
    public void testOnEvent3() throws Exception {
        AtomicReference<TestData> ackDataRef = new AtomicReference<>();
        socket.emit(TestOnEventController.EVENT_NAME_3, null, new Ack() {
            @Override
            public void call(Object... objects) {
                TestData testData = null;
                try {
                    testData = objectMapper.readValue(objects[0].toString(), TestData.class);
                } catch (JsonProcessingException e) {
                    throw new RuntimeException(e);
                }
                ackDataRef.set(testData);
            }
        });
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> testOnEventController.counter.get() == 1
                        && testOnEventController.params.size() == 2
                        && ackDataRef.get() != null);
        assertEquals(1, testOnEventController.counter.get(),
                "onEvent3 methods should be called");
        assertEquals(2, testOnEventController.params.size(),
                "onEvent3 method should have SocketIOClient and AckRequest parameters");
        assertTrue(SocketIOClient.class.isAssignableFrom(testOnEventController.params.get(0).getClass()),
                "First parameter should be of type SocketIOClient");
        assertTrue(AckRequest.class.isAssignableFrom(testOnEventController.params.get(1).getClass()),
                "Second parameter should be of type AckRequest");
        assertEquals(TestData.TEST_ACK_DATA, ackDataRef.get(), "Ack data should match");
    }

    @Test
    public void testOnEvent4() throws Exception {
        AtomicReference<TestData> ackDataRef = new AtomicReference<>();
        socket.emit(TestOnEventController.EVENT_NAME_4,
                objectMapper.writeValueAsString(TestData.TEST_REQ_DATA),
                new Ack() {
                    @Override
                    public void call(Object... objects) {
                        TestData testData = null;
                        try {
                            testData = objectMapper.readValue(objects[0].toString(), TestData.class);
                        } catch (JsonProcessingException e) {
                            throw new RuntimeException(e);
                        }
                        ackDataRef.set(testData);
                    }
                });
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> testOnEventController.counter.get() == 1
                        && testOnEventController.params.size() == 3
                        && ackDataRef.get() != null);
        assertEquals(1, testOnEventController.counter.get(),
                "onEvent4 methods should be called");
        assertEquals(3, testOnEventController.params.size(),
                "onEvent4 method should have AckRequest, SocketIOClient and TestData parameters");
        assertTrue(AckRequest.class.isAssignableFrom(testOnEventController.params.get(0).getClass()),
                "First parameter should be of type AckRequest");
        assertTrue(SocketIOClient.class.isAssignableFrom(testOnEventController.params.get(1).getClass()),
                "Second parameter should be of type SocketIOClient");
        assertTrue(String.class.isAssignableFrom(testOnEventController.params.get(2).getClass()),
                "Third parameter should be of type String");
        assertEquals(TestData.TEST_REQ_DATA, objectMapper.readValue(testOnEventController.params.get(2).toString(), TestData.class), "TestData parameter should match");
        assertEquals(TestData.TEST_ACK_DATA, ackDataRef.get(), "Ack data should match");
    }
}
