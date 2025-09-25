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
package com.corundumstudio.socketio.quarkus.config;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration properties for Netty SocketIO HTTP request decoder in Quarkus application.
 * These properties can be set in the application.properties file with the prefix "netty-socketio.http-request-decoder".
 *
 * @see com.corundumstudio.socketio.HttpRequestDecoderConfiguration
 */
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "netty-socketio.http-request-decoder")
public interface NettySocketIOHttpRequestDecoderConfigMapping {
    /**
     * Maximum length of the initial line (e.g. "GET / HTTP/1.1") in bytes.
     * @see com.corundumstudio.socketio.HttpRequestDecoderConfiguration#getMaxInitialLineLength()
     */
    @WithDefault("4096")
    int maxInitialLineLength();

    /**
     * Maximum size of all headers in bytes.
     * @see com.corundumstudio.socketio.HttpRequestDecoderConfiguration#getMaxHeaderSize()
     */
    @WithDefault("8192")
    int maxHeaderSize();

    /**
     * Maximum size of a single chunk in bytes.
     * @see com.corundumstudio.socketio.HttpRequestDecoderConfiguration#getMaxChunkSize()
     */
    @WithDefault("8192")
    int maxChunkSize();
}
