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
package com.corundumstudio.socketio.protocol;

import io.socket.parser.IOParser;
import io.socket.parser.Packet;

import java.util.concurrent.atomic.AtomicReference;

public class NativeSocketIOClientUtil {
    private static final IOParser.Encoder ENCODER = new IOParser.Encoder();

    /**
     * Converts a Socket.IO packet to a native message format.
     * @param packet
     * @return
     */
    public static String getNativeMessage(Packet packet) {
        AtomicReference<String> result = new AtomicReference<>();
        ENCODER.encode(packet, encodedPackets -> {
            for (Object pack : encodedPackets) {
                io.socket.engineio.parser.Packet<String> enginePacket = new io.socket.engineio.parser.Packet<String>(io.socket.engineio.parser.Packet.MESSAGE);
                if (pack instanceof String) {
                    enginePacket.data = (String)pack;
                    io.socket.engineio.parser.Parser.encodePacket(enginePacket, data -> {
                        result.set(data.toString());
                    });
                }
            }
        });
        return result.get();
    }

    /**
     * Gets the pure Socket.IO protocol encoding without Engine.IO wrapper.
     * This method returns the raw Socket.IO packet format as specified in the protocol documentation.
     * @param packet
     * @return
     */
    public static String getSocketIOProtocolEncoding(Packet packet) {
        AtomicReference<String> result = new AtomicReference<>();
        ENCODER.encode(packet, encodedPackets -> {
            for (Object pack : encodedPackets) {
                if (pack instanceof String) {
                    result.set((String) pack);
                    break; // Take the first encoded packet (Socket.IO format)
                }
            }
        });
        return result.get();
    }
}
