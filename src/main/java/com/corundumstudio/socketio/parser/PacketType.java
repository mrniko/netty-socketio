/**
 * Copyright 2012 Nikita Koksharov
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
package com.corundumstudio.socketio.parser;

public enum PacketType {

    DISCONNECT(0), CONNECT(1), HEARTBEAT(2), MESSAGE(3), JSON(4), EVENT(5), ACK(6), ERROR(7), NOOP(8);

    // optimization founded by profiler
    private static final PacketType[] someValues = values();
    private final int value;

    PacketType(int value) {
        this.value = value;
    }

    public int getValue() {
        return value;
    }

    public static PacketType valueOf(int value) {
        return someValues[value];
    }

}
