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

import java.util.Optional;

import io.quarkus.runtime.annotations.ConfigPhase;
import io.quarkus.runtime.annotations.ConfigRoot;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

/**
 * Configuration properties for Netty SocketIO SSL in Quarkus application.
 * These properties can be set in the application.properties file with the prefix "netty-socketio.ssl".
 *
 * @see com.corundumstudio.socketio.SocketSslConfig
 */
@ConfigRoot(phase = ConfigPhase.BUILD_AND_RUN_TIME_FIXED)
@ConfigMapping(prefix = "netty-socketio.ssl")
public interface NettySocketIOSslConfigMapping {
    /**
     * keystore format. Default is JKS.
     * @see com.corundumstudio.socketio.SocketSslConfig#getKeyStoreFormat()
     */
    @WithDefault("JKS")
    String keyStoreFormat();

    /**
     * keystore password
     * @see com.corundumstudio.socketio.SocketSslConfig#getKeyStorePassword()
     */
    Optional<String> keyStorePassword();

    /**
     * keystore path
     * @see com.corundumstudio.socketio.SocketSslConfig#getKeyStore()
     */
    Optional<String> keyStore();

    /**
     * truststore format. Default is JKS.
     * @see com.corundumstudio.socketio.SocketSslConfig#getTrustStoreFormat()
     */
    @WithDefault("JKS")
    String trustStoreFormat();

    /**
     * truststore password
     * @see com.corundumstudio.socketio.SocketSslConfig#getTrustStorePassword()
     */
    Optional<String> trustStorePassword();

    /**
     * truststore path
     * @see com.corundumstudio.socketio.SocketSslConfig#getTrustStore()
     */
    Optional<String> trustStore();

    /**
     * ssl protocol
     * @see com.corundumstudio.socketio.SocketSslConfig#getSSLProtocol()
     */
    @WithDefault("TLSv1")
    String sslProtocol();

    /**
     * key manager factory algorithm
     * @see com.corundumstudio.socketio.SocketSslConfig#getKeyManagerFactoryAlgorithm()
     */
    Optional<String> keyManagerFactoryAlgorithm();

}
