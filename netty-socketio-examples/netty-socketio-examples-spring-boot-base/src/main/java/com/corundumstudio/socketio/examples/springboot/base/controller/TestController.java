package com.corundumstudio.socketio.examples.springboot.base.controller;

import java.util.concurrent.atomic.AtomicReference;

import org.springframework.stereotype.Component;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.annotation.OnConnect;

/**
 * Test controller to demonstrate Socket.IO event handling in a Spring Boot application.
 * This controller listens for client connections and stores the connected client reference.
 * It throws a RuntimeException in the onConnect method to simulate an error scenario.
 */
@Component
public class TestController {
    AtomicReference<SocketIOClient> baseClient = new AtomicReference<>();

    public SocketIOClient getBaseClient() {
        return baseClient.get();
    }

    @OnConnect
    public void onConnect(SocketIOClient client) {
        baseClient.set(client);
        throw new RuntimeException("onConnect");
    }
}
