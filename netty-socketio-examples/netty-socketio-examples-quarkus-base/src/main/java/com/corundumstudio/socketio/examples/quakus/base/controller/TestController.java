package com.corundumstudio.socketio.examples.quakus.base.controller;

import java.util.concurrent.atomic.AtomicReference;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.annotation.OnConnect;

import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;

/**
 * Test controller to demonstrate Socket.IO event handling in a Quarkus application.
 * This controller listens for client connections and stores the connected client reference.
 * It throws a RuntimeException in the onConnect method to simulate an error scenario.
 * @Unremovable ensures that this bean is not removed during build optimization.
 */
@Unremovable
@ApplicationScoped
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
