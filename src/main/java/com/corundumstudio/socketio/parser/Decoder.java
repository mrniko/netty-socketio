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
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.util.CharsetUtil;

public class Decoder {

    private final byte separator = (byte)':';

    private final Pattern packetPattern = Pattern.compile("([^:]+):([0-9]+)?(\\+)?:([^:]+)?:?([\\s\\S]*)?");
    private final Pattern ackPattern = Pattern.compile("^([0-9]+)(\\+)?(.*)");

    private final ObjectMapper objectMapper;

    public Decoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

/*
    public List<Packet> decodePayload(String data) throws IOException {
        if (data.isEmpty()) {
            return Collections.emptyList();
        }
        // TODO make it async
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
*/
    // fastest way to parse chars to int
    public Integer parseInt(byte[] chars) {
		int result = 0;
		for (int i = 0; i < chars.length; i++) {
			int digit = ((int)chars[i] & 0xF);
	    	for (int j = 0; j < chars.length-1-i; j++) {
	    		digit *= 10;
			}
	    	result += digit;
		}
    	return result;
    }

    public Packet decodePacket(ChannelBuffer buffer) throws IOException {
        if (buffer.readableBytes() < 3) {
            throw new DecoderException("Can't parse " + buffer.toString(CharsetUtil.UTF_8));
        }
        PacketType type = getType(buffer);

        int readerIndex = 1;
        // 'null' to avoid unnecessary StringBuilder creation
        StringBuilder messageId = null;
        for (readerIndex += 1; readerIndex < buffer.readableBytes(); readerIndex++) {
            if (messageId == null) {
                messageId = new StringBuilder(4);
            }
            byte msg = buffer.getByte(readerIndex);
            if (msg == separator) {
                break;
            }
            if (msg != (byte)'+') {
                messageId.append((char)msg);
            }
        }
        Integer id = null;
        if (messageId != null && messageId.length() > 0) {
            id = Integer.valueOf(messageId.toString());
        }

        // 'null' to avoid unnecessary StringBuilder creation
        StringBuilder endpointBuffer = null;
        for (readerIndex += 1; readerIndex < buffer.readableBytes(); readerIndex++) {
            if (endpointBuffer == null) {
                endpointBuffer = new StringBuilder();
            }
            byte msg = buffer.getByte(readerIndex);
            if (msg == separator) {
                break;
            }
            endpointBuffer.append((char)msg);
        }

        String endpoint = null;
        if (endpointBuffer != null && endpointBuffer.length() > 0) {
            endpoint = endpointBuffer.toString();
        }

        readerIndex += 1;
        byte[] data = null;
        if (buffer.readableBytes() - readerIndex > 0) {
            data = new byte[buffer.readableBytes() - readerIndex];
            buffer.getBytes(readerIndex, data);
        }

        Packet packet = new Packet(type);
        packet.setEndpoint(endpoint);
        if (id != null) {
            packet.setId(id);
            if (data != null) {
                packet.setAck("data");
            } else {
                packet.setAck(true);
            }
        }

        switch (type) {
        case ERROR:
            if (data == null) {
                break;
            }
            String[] pieces = new String(data).split("\\+");
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
                packet.setData(new String(data));
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
            if (data != null) {
                packet.setQs(new String(data));
            }
            break;

        case ACK:
            if (data == null) {
                break;
            }
            Matcher ackMatcher = ackPattern.matcher(new String(data));
            if (ackMatcher.matches()) {
                packet.setAckId(ackMatcher.group(1));
                String ackArgsJSON = extract(ackMatcher, 3);
                if (ackArgsJSON != null && ackArgsJSON.trim().length() > 0) {
                    @SuppressWarnings("unchecked")
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

    private PacketType getType(ChannelBuffer buffer) {
        int typeId = buffer.getByte(0) & 0xF;
        if (typeId >= PacketType.VALUES.length
                || buffer.getByte(1) != separator) {
            throw new DecoderException("Can't parse " + buffer.toString(CharsetUtil.UTF_8));
        }
        return PacketType.valueOf(typeId);
    }

    private String extract(Matcher matcher, int index) {
        if (index > matcher.groupCount()) {
            return null;
        }
        return matcher.group(index);
    }

    public Packet decodePacket(String string) throws IOException {
        return decodePacket(ChannelBuffers.copiedBuffer(string, CharsetUtil.UTF_8));
    }

}
