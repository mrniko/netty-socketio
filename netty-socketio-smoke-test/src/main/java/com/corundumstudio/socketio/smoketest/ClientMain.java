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
package com.corundumstudio.socketio.smoketest;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.javafaker.Faker;

import io.socket.client.IO;
import io.socket.client.Socket;

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.awaitility.Awaitility.await;

/**
 * SocketIO Client for smoke testing.
 * Sends messages and measures performance.
 */
public class ClientMain {
    
    private static final Logger log = LoggerFactory.getLogger(ClientMain.class);
    private static final ObjectMapper mapper = new ObjectMapper();
    private static final Faker faker = new Faker();
    
    private final List<Socket> clients = new ArrayList<>();
    private final ClientMetrics metrics;
    private final AtomicBoolean running = new AtomicBoolean(false);
    private final AtomicInteger connectedCount = new AtomicInteger(0);
    private final SystemInfo systemInfo = new SystemInfo();
    private final int port;
    private final int clientCount;
    private final int eachMsgCount;
    private final int eachMsgSize;

    public ClientMain(int port, int clientCount, int eachMsgCount, int eachMsgSize, ClientMetrics metrics) throws Exception {
        this.port = port;
        this.clientCount = clientCount;
        this.eachMsgCount = eachMsgCount;
        this.eachMsgSize = eachMsgSize;
        this.metrics = metrics;
    }
    
    public void start() throws Exception {
        systemInfo.printSystemInfo();
        
        // Connect all clients
        connectClients();
        
        // Wait for all clients to connect
        long timeout = System.currentTimeMillis() + 30000; // 30 seconds timeout
        while (connectedCount.get() < clientCount && System.currentTimeMillis() < timeout) {
            try {
                Thread.sleep(100);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
        
        if (connectedCount.get() < clientCount) {
            throw new RuntimeException("Failed to connect all clients. Connected: " + connectedCount.get() + "/" +clientCount);
        }
        
        log.info("All {} clients connected", clientCount);
        
        // Start sending messages
        startMessageSending();
        
        // Cleanup
        cleanup();
    }
    
    public ClientMetrics getMetrics() {
        return metrics;
    }
    
    private void connectClients() {
        String serverUrl = String.format("http://127.0.0.1:%d", port);
        
        for (int i = 0; i < clientCount; i++) {
            try {
                IO.Options options = new IO.Options();

                Socket client = IO.socket(URI.create(serverUrl), options);
                clients.add(client);
                log.info("Client {} connecting to {}", i, serverUrl);
                client.connect();
                int finalI = i;
                client.on(Socket.EVENT_CONNECT, args -> {
                    int count = connectedCount.incrementAndGet();
                    log.info("Client {} connected (total connected: {})", finalI, count);
                });
                client.on(Socket.EVENT_DISCONNECT, args -> {
                    int count = connectedCount.decrementAndGet();
                    log.info("Client {} disconnected (total connected: {})", finalI, count);
                });
            } catch (Exception e) {
                log.error("Failed to create client {}", i, e);
                metrics.recordError();
            }
        }
    }
    
    private void startMessageSending() throws InterruptedException {
        running.set(true);
        metrics.start();
        
        Thread[] threads = new Thread[clientCount];
        for (int i = 0; i < threads.length; i++) {
            int finalI = i;
            threads[i] = new Thread(() -> {
                Socket socket = clients.get(finalI);
                for (int j = 0; j < eachMsgCount; j++) {
                    if (!running.get()) {
                        break;
                    }
                    sendMessage(socket);
                }
            });
        }
        for (Thread thread : threads) {
            thread.start();
        }
        for (Thread thread : threads) {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        await().atMost(10, TimeUnit.MINUTES).until(() ->
                metrics.getTotalMessagesSent() == metrics.getTotalMessagesReceived()
        );
        metrics.stop();
        log.info(metrics.toString());
    }
    
    private void sendMessage(Socket client) {
        try {
            String message = generateMessage(eachMsgSize);
            long startTime = System.currentTimeMillis();
            
            // Record message sent
            metrics.recordMessageSent(message.length());
            
            // Send without ACK callback
            client.emit("echo", startTime + ":" + message);
            
        } catch (Exception e) {
            log.debug("Failed to send message", e);
            metrics.recordError();
        }
    }
    
    private String generateMessage(int size) {
        return faker.lorem().characters(size);
    }
    
    private void cleanup() {
        log.info("Cleaning up clients...");
        
        for (Socket client : clients) {
            try {
                if (client.connected()) {
                    client.disconnect();
                }
                client.close();
            } catch (Exception e) {
                log.debug("Error closing client", e);
            }
        }
    }
}
