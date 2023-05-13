package com.corundumstudio.socketio.protocol;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

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

    UNKNOWN(""),
    ;

    public static final String EIO = "EIO";

    private static final Map<String, EngineIOVersion> VERSIONS = new HashMap<>();

    static {
        for (EngineIOVersion value : values()) {
            VERSIONS.put(value.getValue(), value);
        }
    }

    private final String value;

    private EngineIOVersion(String value) {
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
