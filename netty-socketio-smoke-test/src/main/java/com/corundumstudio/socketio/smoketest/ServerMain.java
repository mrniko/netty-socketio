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

import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.Option;
import org.apache.commons.cli.Options;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.SocketIOServer;
import com.corundumstudio.socketio.listener.DataListener;
import com.fasterxml.jackson.databind.ObjectMapper;

/**
 * SocketIO Server for smoke testing.
 */
public class ServerMain {
    public static final int DEFAULT_PORT = 8899;

    private static final Logger log = LoggerFactory.getLogger(ServerMain.class);
    private static final ObjectMapper mapper = new ObjectMapper();

    private SocketIOServer server;
    private final SystemInfo systemInfo = new SystemInfo();
    private final ClientMetrics clientMetrics;

    public ServerMain(ClientMetrics clientMetrics) {
        this.clientMetrics = clientMetrics;
    }

    public void start(int port) throws Exception {
        systemInfo.printSystemInfo();
        log.info("Starting SocketIO server with port: {}", port);

        Configuration serverConfig = new Configuration();
        serverConfig.setPort(port);
        server = new SocketIOServer(serverConfig);
        setupEventListeners();

        server.start();
        log.info("SocketIO server started at port: {}", port);
    }

    private void setupEventListeners() {
        // Echo listener - echoes back all received messages
        server.addEventListener("echo", String.class, new DataListener<String>() {
            @Override
            public void onData(com.corundumstudio.socketio.SocketIOClient client, String data,
                               com.corundumstudio.socketio.AckRequest ackRequest) throws Exception {
                String time = data.split(":")[0];
                long startTime = Long.parseLong(time);
                long rtt = System.currentTimeMillis() - startTime;
                clientMetrics.recordLatency(rtt);
                clientMetrics.recordMessageReceived(data.length());
            }
        });
    }

    public void stop() {
        if (server != null) {
            log.info("Stopping SocketIO server...");
            server.stop();
        }
    }
}
