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
import java.util.Iterator;
import java.util.List;

import org.codehaus.jackson.map.ObjectMapper;

public class Encoder {

    private final ObjectMapper objectMapper;

    public Encoder(ObjectMapper objectMapper) {
        super();
        this.objectMapper = objectMapper;
    }

    public CharSequence encodePayload(List<String> packets) {
        if (packets.size() == 1) {
            return packets.get(0);
        }
        StringBuilder result = new StringBuilder();
        for (String packet : packets) {
            result.append(Packet.DELIMITER).append(packet.length()).append(Packet.DELIMITER).append(packet);
        }
        return result;
    }

    public String encodePackets(List<Packet> packets) throws IOException {
        if (packets.size() == 1) {
            return encodePacket(packets.get(0));
        }
        StringBuilder result = new StringBuilder();
        for (Packet packet : packets) {
            String encPacket = encodePacket(packet);
            result.append(Packet.DELIMITER).append(encPacket.length()).append(Packet.DELIMITER).append(encPacket);
        }
        return result.toString();
    }

    public String encodePacket(Packet packet) throws IOException {
        int type = packet.getType().getValue();
        Integer id = packet.getId();
        String endpoint = packet.getEndpoint();
        Object ack = packet.getAck();
        Object data = packet.getData();

        switch (packet.getType()) {

        case MESSAGE:
            if (packet.getData() != null) {
                data = packet.getData();
            }
            break;

        case EVENT:
            List<?> args = packet.getArgs();
            if (args.isEmpty()) {
                args = null;
            }
            Event event = new Event(packet.getName(), args);
            data = objectMapper.writeValueAsString(event);
            break;

        case JSON:
            data = objectMapper.writeValueAsString(packet.getData());
            break;

        case CONNECT:
            data = packet.getQs();
            break;

        case ACK:
            String dataStr = packet.getAckId();
            if (!packet.getArgs().isEmpty()) {
                dataStr += "+" + objectMapper.writeValueAsString(packet.getArgs());
            }
            data = dataStr;
            break;

        case ERROR:
            int reasonCode = -1;
            int adviceCode = -1;
            if (packet.getReason() != null) {
                reasonCode = packet.getReason().getValue();
            }
            if (packet.getAdvice() != null) {
                adviceCode = packet.getAdvice().getValue();
            }

            if (reasonCode != -1 || adviceCode != -1) {
                StringBuilder errorData = new StringBuilder();
                if (reasonCode != -1) {
                    errorData.append(reasonCode);
                }
                if (adviceCode != -1) {
                    errorData.append("+").append(adviceCode);
                }
                data = errorData;
            }
            break;

        }

        List<Object> params = new ArrayList<Object>(4);
        params.add(type);
        if ("data".equals(ack)) {
            params.add(id + "+");
        } else {
            if (id == null) {
                params.add("");
            } else {
                params.add(id);
            }
        }
        params.add(endpoint);
        if (data != null) {
            params.add(data);
        }

        return join(":", params);
    }

    private String join(String delimiter, List<Object> args) {
        StringBuilder result = new StringBuilder();
        for (Iterator<Object> iterator = args.iterator(); iterator.hasNext();) {
            Object arg = iterator.next();
            result.append(arg);
            if (iterator.hasNext()) {
                result.append(delimiter);
            }
        }
        return result.toString();
    }

}
