package com.corundumstudio.socketio.examples.quakus.base.controller;

import java.util.concurrent.atomic.AtomicReference;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.annotation.OnConnect;

import io.quarkus.arc.Unremovable;
import jakarta.enterprise.context.ApplicationScoped;

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
