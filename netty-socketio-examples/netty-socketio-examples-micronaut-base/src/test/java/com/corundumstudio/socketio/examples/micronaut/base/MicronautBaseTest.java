package com.corundumstudio.socketio.examples.micronaut.base;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.examples.micronaut.base.config.CustomizedSocketIOConfiguration;
import com.corundumstudio.socketio.examples.micronaut.base.controller.TestController;

import io.micronaut.test.extensions.junit5.annotation.MicronautTest;
import io.socket.client.IO;
import io.socket.client.Socket;
import jakarta.inject.Inject;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@MicronautTest
public class MicronautBaseTest {

    @Inject
    SocketIOServer socketIOServer;
    @Inject
    CustomizedSocketIOConfiguration customizedSocketIOConfiguration;
    @Inject
    TestController testController;

    private Socket socket;

    @org.junit.jupiter.api.AfterEach
    public void tearDown() {
        if (socket != null) {
            socket.close();
        }
    }

    @Test
    public void testSocketIOServerConnect() throws Exception {
        // wait for server start
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> socketIOServer != null && socketIOServer.isStarted());

        socket = IO.socket("http://localhost:9202");
        socket.connect();

        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> socket.connected());
        await().atMost(5, TimeUnit.SECONDS)
                .until(() -> testController.getBaseClient() != null && customizedSocketIOConfiguration.getExceptionListener() != null);

        SocketIOClient baseClient = testController.getBaseClient();
        assertNotNull(baseClient);
        Throwable lastException = customizedSocketIOConfiguration.getLastException();
        assertNotNull(lastException);
        assertInstanceOf(RuntimeException.class, lastException);
    }

}
