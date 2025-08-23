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
package com.corundumstudio.socketio.store;

import com.github.dockerjava.api.command.InspectContainerResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.testcontainers.containers.GenericContainer;

import java.util.concurrent.TimeUnit;

/**
 * Customized Hazelcast container for testing purposes.
 */
public class CustomizedHazelcastContainer extends GenericContainer<CustomizedHazelcastContainer> {
    private static final Logger log = LoggerFactory.getLogger(CustomizedHazelcastContainer.class);
    public static final int HAZELCAST_PORT = 5701;

    /**
     * Default constructor that initializes the Hazelcast container with the official Hazelcast image.
     */
    public CustomizedHazelcastContainer() {
        super("hazelcast/hazelcast:3.12.12");
    }

    @Override
    protected void configure() {
        withExposedPorts(HAZELCAST_PORT);
        withEnv("JVM_OPTS", "-Dhazelcast.config=/opt/hazelcast/config_ext/hazelcast.xml");
        withClasspathResourceMapping("hazelcast-test-config.xml", 
            "/opt/hazelcast/config_ext/hazelcast.xml", 
            org.testcontainers.containers.BindMode.READ_ONLY);
    }

    @Override
    protected void containerIsStarted(InspectContainerResponse containerInfo) {
        try {
            // Wait for Hazelcast to be ready
            TimeUnit.SECONDS.sleep(15);
            
            // Check if Hazelcast is responding
            ExecResult result = null;
            int attempts = 0;
            while (attempts < 20) {
                try {
                    result = execInContainer("sh", "-c", "netstat -an | grep " + HAZELCAST_PORT + " | grep LISTEN");
                    if (result.getExitCode() == 0 && result.getStdout().contains("LISTEN")) {
                        log.info("Hazelcast is ready and listening on port {}", HAZELCAST_PORT);
                        break;
                    }
                } catch (Exception e) {
                    // Ignore and retry
                }
                
                attempts++;
                TimeUnit.SECONDS.sleep(2);
                log.info("Waiting for Hazelcast to be ready, attempt {}", attempts);
            }
            
            if (attempts >= 20) {
                log.info("Hazelcast container started but may not be fully ready");
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to start Hazelcast container", e);
        }
    }

    @Override
    public void start() {
        super.start();
        log.info("Hazelcast started at port: {}", getHazelcastPort());
    }

    @Override
    public void stop() {
        super.stop();
        log.info("Hazelcast stopped");
    }

    public int getHazelcastPort() {
        return getMappedPort(HAZELCAST_PORT);
    }
}
