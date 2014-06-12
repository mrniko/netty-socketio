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

    OPEN(0), CLOSE(1), PING(2), PONG(3), MESSAGE(4), UPGRADE(5), NOOP(6),

    CONNECT(0, true), DISCONNECT(1, true), EVENT(2, true), ACK(3, true), ERROR(4, true), BINARY_EVENT(5, true);

    private static final PacketType[] VALUES = values();
    private final int value;
    private final boolean inner;

    PacketType(int value) {
        this(value, false);
    }

    PacketType(int value, boolean inner) {
        this.value = value;
        this.inner = inner;
    }

    public int getValue() {
        return value;
    }

    public static PacketType valueOf(int value) {
        for (PacketType type : VALUES) {
            if (type.getValue() == value && !type.inner) {
                return type;
            }
        }
        throw new IllegalStateException();
    }

    public static PacketType valueOfInner(int value) {
        for (PacketType type : VALUES) {
            if (type.getValue() == value && type.inner) {
                return type;
            }
        }
        throw new IllegalStateException();
    }

}
