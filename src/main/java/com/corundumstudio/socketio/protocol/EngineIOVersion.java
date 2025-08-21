/**
 * Copyright (c) 2012-2023 Nikita Koksharov
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
package com.corundumstudio.socketio.protocol;

import java.util.HashMap;
import java.util.Map;

/**
 * Engine.IO protocol version
 */
public enum EngineIOVersion {
    /**
     * @link <a href="https://github.com/socketio/engine.io-protocol/tree/v2">Engine.IO version 2</a>
     */
    V2("2"),
    /**
     * @link <a href="https://github.com/socketio/engine.io-protocol/tree/v3">Engine.IO version 3</a>
     */
    V3("3"),
    /**
     * current version
     * @link <a href="https://github.com/socketio/engine.io-protocol/tree/main">Engine.IO version 4</a>
     */
    V4("4"),

    UNKNOWN("");

    public static final String EIO = "EIO";

    private static final Map<String, EngineIOVersion> VERSIONS = new HashMap<>();

    static {
        for (EngineIOVersion value : values()) {
            VERSIONS.put(value.getValue(), value);
        }
    }

    private final String value;

    EngineIOVersion(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }

    public static EngineIOVersion fromValue(String value) {
        EngineIOVersion engineIOVersion = VERSIONS.get(value);
        if (engineIOVersion != null) {
            return engineIOVersion;
        }
       return UNKNOWN;
    }
}
