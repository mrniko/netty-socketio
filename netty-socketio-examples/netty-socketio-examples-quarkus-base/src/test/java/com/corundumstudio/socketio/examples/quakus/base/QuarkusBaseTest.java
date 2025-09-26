package com.corundumstudio.socketio.examples.quakus.base;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.examples.quakus.base.config.CustomizedSocketIOConfiguration;
import com.corundumstudio.socketio.examples.quakus.base.controller.TestController;

import io.quarkus.test.junit.QuarkusTest;
import io.quarkus.test.junit.QuarkusTestProfile;
import io.quarkus.test.junit.TestProfile;
import io.socket.client.IO;
import io.socket.client.Socket;
import jakarta.inject.Inject;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.wildfly.common.Assert.assertNotNull;

@QuarkusTest
@TestProfile(QuarkusBaseTest.TestProfile.class)
public class QuarkusBaseTest {
    public static class TestProfile implements QuarkusTestProfile {
        @Override
        public Map<String, String> getConfigOverrides() {
            return new HashMap<>();
        }
    }

    @Inject
    SocketIOServer socketIOServer;
    @Inject
    CustomizedSocketIOConfiguration customizedSocketIOConfiguration;
    @Inject
    TestController testController;

    private Socket socket;

    @Test
    public void testSocketIOServerConnect() throws Exception {
        // wait for server start
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> socketIOServer != null && socketIOServer.isStarted());

        socket = IO.socket("http://localhost:9201");
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
