package com.corundumstudio.socketio.examples.springboot.base;

import java.util.concurrent.TimeUnit;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import com.corundumstudio.socketio.SocketIOClient;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.examples.springboot.base.config.CustomizedSocketIOConfiguration;
import com.corundumstudio.socketio.examples.springboot.base.controller.TestController;

import io.socket.client.IO;
import io.socket.client.Socket;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
public class SpringBootBaseTest {

    @Autowired
    SocketIOServer socketIOServer;
    @Autowired
    CustomizedSocketIOConfiguration customizedSocketIOConfiguration;
    @Autowired
    TestController testController;

    private Socket socket;

    @Test
    public void testSocketIOServerConnect() throws Exception {
        // wait for server start
        await().atMost(10, TimeUnit.SECONDS)
                .until(() -> socketIOServer != null && socketIOServer.isStarted());

        socket = IO.socket("http://localhost:9200");
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
