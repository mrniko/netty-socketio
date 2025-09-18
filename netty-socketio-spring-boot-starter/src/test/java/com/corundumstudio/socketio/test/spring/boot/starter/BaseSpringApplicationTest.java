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
package com.corundumstudio.socketio.test.spring.boot.starter;

import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;

import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.RANDOM_PORT;

@SpringBootTest(
        webEnvironment = RANDOM_PORT,
        classes = BaseSpringApplicationTest.TestApplication.class
)
public abstract class BaseSpringApplicationTest {
    /**
     * scanBasePackages must not be the same as the package of main code
     * to avoid component scanning of main code
     * because it would be loaded by spring factories mechanism
     */
    @SpringBootApplication(scanBasePackages = "com.corundumstudio.socketio.test.spring.boot.starter")
    public static class TestApplication {
    }

}
