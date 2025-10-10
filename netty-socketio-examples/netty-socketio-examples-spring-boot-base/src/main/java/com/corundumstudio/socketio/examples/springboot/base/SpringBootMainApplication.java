/**
 * Copyright (c) 2012-2025 Nikita Koksharov
 * <p>
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * <p>
 * http://www.apache.org/licenses/LICENSE-2.0
 * <p>
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.corundumstudio.socketio.examples.springboot.base;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import com.corundumstudio.socketio.SocketIOServer;

@SpringBootApplication
public class SpringBootMainApplication implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(SpringBootMainApplication.class);
    @Autowired
    private SocketIOServer server;

    public static void main(String[] args) {
        SpringApplication.run(SpringBootMainApplication.class, args);
    }

    @Override
    public void run(String... args) throws Exception {
        log.info("socket.io server started: {}", server.isStarted());
    }
}
