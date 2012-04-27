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

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jackson.map.ObjectMapper;

public class Decoder {

    private final Pattern packetPattern = Pattern.compile("([^:]+):([0-9]+)?(\\+)?:([^:]+)?:?([\\s\\S]*)?");
    private final Pattern ackPattern = Pattern.compile("^([0-9]+)(\\+)?(.*)");

    private final ObjectMapper objectMapper;

    public Decoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public List<Packet> decodePayload(String data) throws IOException {
        if (data.isEmpty()) {
            return Collections.emptyList();
        }
        List<Packet> result = new ArrayList<Packet>();
        if (data.charAt(0) == Packet.DELIMITER) {
            // TODO use ForkJoin
            StringBuilder length = new StringBuilder(4);
            for (int i = 1; i < data.length(); i++) {
                if (data.charAt(i) == Packet.DELIMITER) {
                    Integer len = Integer.valueOf(length.toString());
                    String packetStr = data.substring(i + 1, i + 1 + len);
                    Packet packet = decodePacket(packetStr);
                    result.add(packet);
                    i += len + 1;
                    length.setLength(0);
                } else {
                    length.append(data.charAt(i));
                }
            }
        } else {
            result.add(decodePacket(data));
        }
        return result;

    }

    public Packet decodePacket(String msg) throws IOException {
        Matcher matcher = packetPattern.matcher(msg);
        if (!matcher.matches()) {
            return Packet.NULL_INSTANCE;
        }
        String id = extract(matcher, 2);
        String data = extract(matcher, 5);
        int typeId = Integer.valueOf(matcher.group(1));
        PacketType type = PacketType.valueOf(typeId);
        String endpoint = extract(matcher, 4);

        Packet packet = new Packet(type);
        packet.setEndpoint(endpoint);
        packet.setId(id);
        if (id != null) {
            String ackData = extract(matcher, 3);
            if (ackData != null) {
                packet.setAck("data");
            } else {
                packet.setAck(true);
            }
        }

        switch (type) {
        case ERROR:
            String[] pieces = data.split("\\+");
            if (pieces.length > 0 && pieces[0].trim().length() > 0) {
                ErrorReason reason = ErrorReason.valueOf(Integer.valueOf(pieces[0]));
                packet.setReason(reason);
                if (pieces.length > 1) {
                    ErrorAdvice advice = ErrorAdvice.valueOf(Integer.valueOf(pieces[1]));
                    packet.setAdvice(advice);
                }
            }
            break;

        case MESSAGE:
            if (data != null) {
                packet.setData(data);
            } else {
                packet.setData("");
            }
            break;

        case EVENT:
            Event event = objectMapper.readValue(data, Event.class);
            packet.setName(event.getName());
            if (event.getArgs() != null) {
                packet.setArgs(event.getArgs());
            }
            break;

        case JSON:
            Object obj = objectMapper.readValue(data, Object.class);
            packet.setData(obj);
            break;

        case CONNECT:
            packet.setQs(data);
            break;

        case ACK:
            if (data == null) {
                break;
            }
            Matcher ackMatcher = ackPattern.matcher(data);
            if (ackMatcher.matches()) {
                packet.setAckId(ackMatcher.group(1));
                String ackArgsJSON = extract(ackMatcher, 3);
                if (ackArgsJSON != null && ackArgsJSON.trim().length() > 0) {
                    List<Object> args = objectMapper.readValue(ackArgsJSON, List.class);
                    packet.setArgs(args);
                }
            }
            break;

        case DISCONNECT:
        case HEARTBEAT:
            break;
        }

        return packet;
    }

    private String extract(Matcher matcher, int index) {
        if (index > matcher.groupCount()) {
            return null;
        }
        return matcher.group(index);
    }

}
