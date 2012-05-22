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

import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferIndexFinder;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.util.CharsetUtil;

public class Decoder {

    private final UTF8CharsScanner charsScanner = new UTF8CharsScanner();

    private final ChannelBufferIndexFinder delimiterFinder = new ChannelBufferIndexFinder() {
        @Override
        public boolean find(ChannelBuffer buffer, int guessedIndex) {
            return isCurrentDelimiter(buffer, guessedIndex);
        }
    };

    private final ObjectMapper objectMapper;

    public Decoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    // fastest way to parse chars to int
    private long parseLong(byte[] chars) {
        return parseLong(chars, chars.length);
    }

    private long parseLong(byte[] chars, int length) {
        long result = 0;
        for (int i = 0; i < length; i++) {
            int digit = ((int)chars[i] & 0xF);
            for (int j = 0; j < length-1-i; j++) {
                digit *= 10;
            }
            result += digit;
        }
        return result;
    }

    private Packet decodePacket(ChannelBuffer buffer) throws IOException {
        if (buffer.readableBytes() < 3) {
            throw new DecoderException("Can't parse " + buffer.toString(CharsetUtil.UTF_8));
        }
        PacketType type = getType(buffer);

        int readerIndex = buffer.readerIndex()+1;
        // 'null' to avoid unnecessary StringBuilder creation
        boolean hasData = false;
        StringBuilder messageId = null;
        for (readerIndex += 1; readerIndex < buffer.readableBytes(); readerIndex++) {
            if (messageId == null) {
                messageId = new StringBuilder(4);
            }
            byte msg = buffer.getByte(readerIndex);
            if (msg == Packet.SEPARATOR) {
                break;
            }
            if (msg != (byte)'+') {
                messageId.append((char)msg);
            } else {
                hasData = true;
            }
        }
        Long id = null;
        if (messageId != null && messageId.length() > 0) {
            id = Long.valueOf(messageId.toString());
        }

        // 'null' to avoid unnecessary StringBuilder creation
        StringBuilder endpointBuffer = null;
        for (readerIndex += 1; readerIndex < buffer.readableBytes(); readerIndex++) {
            if (endpointBuffer == null) {
                endpointBuffer = new StringBuilder();
            }
            byte msg = buffer.getByte(readerIndex);
            if (msg == Packet.SEPARATOR) {
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
            if (hasData) {
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
            boolean validFormat = true;
            int plusIndex = -1;
            for (int i = 0; i < data.length; i++) {
                byte dataChar = data[i];
                if (!Character.isDigit(dataChar)) {
                    if (dataChar == '+') {
                        plusIndex = i;
                        break;
                    } else {
                        validFormat = false;
                        break;
                    }
                }
            }
            if (!validFormat) {
                break;
            }

            if (plusIndex == -1) {
                packet.setAckId(parseLong(data));
                break;
            } else {
                packet.setAckId(parseLong(data, plusIndex));
                buffer.readerIndex(readerIndex+plusIndex+1);
                List<Object> args = objectMapper.readValue(new ChannelBufferInputStream(buffer), List.class);
                packet.setArgs(args);
            }
            break;

        case DISCONNECT:
        case HEARTBEAT:
            break;
        }

        return packet;
    }

    private PacketType getType(ChannelBuffer buffer) {
        int typeId = buffer.getByte(buffer.readerIndex()) & 0xF;
        if (typeId >= PacketType.VALUES.length
                || buffer.getByte(buffer.readerIndex()+1) != Packet.SEPARATOR) {
            throw new DecoderException("Can't parse " + buffer.toString(CharsetUtil.UTF_8));
        }
        return PacketType.valueOf(typeId);
    }

    public Packet decodePacket(String string) throws IOException {
        return decodePacket(ChannelBuffers.copiedBuffer(string, CharsetUtil.UTF_8));
    }

    public Packet decodePackets(ChannelBuffer buffer) throws IOException {
        if (isCurrentDelimiter(buffer, buffer.readerIndex())) {
            buffer.readerIndex(buffer.readerIndex() + Packet.DELIMITER_BYTES.length);

            Integer len = extractLength(buffer);

            ChannelBuffer frame = buffer.slice(buffer.readerIndex(), len);
            Packet packet = decodePacket(frame);
            buffer.readerIndex(buffer.readerIndex() + len);
            return packet;
        } else {
            Packet packet = decodePacket(buffer);
            buffer.readerIndex(buffer.readableBytes());
            return packet;
        }
    }

    private Integer extractLength(ChannelBuffer buffer) {
        int len = (int)parseLengthHeader(buffer);

        // scan utf8 symbols if needed
        if (buffer.capacity() > buffer.readerIndex() + len
                && !isCurrentDelimiter(buffer, buffer.readerIndex() + len)) {
            int index = charsScanner.findTailIndex(buffer, buffer.readerIndex(), buffer.capacity(), len);
            len = index - buffer.readerIndex();
        }
        return len;
    }

    private long parseLengthHeader(ChannelBuffer buffer) {
        int delimiterIndex = ChannelBuffers.indexOf(buffer, buffer.readerIndex(), buffer.capacity(), delimiterFinder);
        if (delimiterIndex == -1) {
            throw new DecoderException("Can't find tail delimiter");
        }

        byte[] digits = new byte[delimiterIndex - buffer.readerIndex()];;
        buffer.getBytes(buffer.readerIndex(), digits);
        buffer.readerIndex(buffer.readerIndex() + digits.length + Packet.DELIMITER_BYTES.length);
        return parseLong(digits);
    }

    private boolean isCurrentDelimiter(ChannelBuffer buffer, int index) {
        for (int i = 0; i < Packet.DELIMITER_BYTES.length; i++) {
            if (buffer.getByte(index + i) != Packet.DELIMITER_BYTES[i]) {
                return false;
            }
        }
        return true;
    }

}
