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
import java.util.UUID;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferIndexFinder;
import org.jboss.netty.buffer.ChannelBufferInputStream;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.util.CharsetUtil;

import com.corundumstudio.socketio.AckCallback;
import com.corundumstudio.socketio.AckManager;
import com.corundumstudio.socketio.namespace.Namespace;

public class Decoder {

    private final UTF8CharsScanner charsScanner = new UTF8CharsScanner();

    private final ChannelBufferIndexFinder delimiterFinder = new ChannelBufferIndexFinder() {
        @Override
        public boolean find(ChannelBuffer buffer, int guessedIndex) {
            return   isCurrentDelimiter(buffer, guessedIndex);
        }
    };

    private final JsonSupport jsonSupport;
    private final AckManager ackManager;

    public Decoder(JsonSupport jsonSupport, AckManager ackManager) {
        this.jsonSupport = jsonSupport;
        this.ackManager = ackManager;
    }

    // fastest way to parse chars to int
    private long parseLong(ChannelBuffer chars) {
        return parseLong(chars, chars.readerIndex() + chars.readableBytes());
    }

    private long parseLong(ChannelBuffer chars, int length) {
        long result = 0;
        for (int i = chars.readerIndex(); i < length; i++) {
            int digit = ((int)chars.getByte(i) & 0xF);
            for (int j = 0; j < length-1-i; j++) {
                digit *= 10;
            }
            result += digit;
        }
        return result;
    }

    private Packet decodePacket(ChannelBuffer buffer, UUID uuid) throws IOException {
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

        String endpoint = Namespace.DEFAULT_NAME;
        if (endpointBuffer != null && endpointBuffer.length() > 0) {
            endpoint = endpointBuffer.toString();
        }

        if (buffer.readableBytes() == readerIndex) {
            buffer.readerIndex(buffer.readableBytes());
        } else {
            readerIndex += 1;
            buffer.readerIndex(readerIndex);
        }

        Packet packet = new Packet(type);
        packet.setEndpoint(endpoint);
        if (id != null) {
            packet.setId(id);
            if (hasData) {
                packet.setAck(Packet.ACK_DATA);
            } else {
                packet.setAck(true);
            }
        }

        switch (type) {
        case ERROR: {
            if (!buffer.readable()) {
                break;
            }
            String[] pieces = buffer.toString(CharsetUtil.UTF_8).split("\\+");
            if (pieces.length > 0 && pieces[0].trim().length() > 0) {
                ErrorReason reason = ErrorReason.valueOf(Integer.valueOf(pieces[0]));
                packet.setReason(reason);
                if (pieces.length > 1) {
                    ErrorAdvice advice = ErrorAdvice.valueOf(Integer.valueOf(pieces[1]));
                    packet.setAdvice(advice);
                }
            }
            break;
        }

        case MESSAGE: {
            if (buffer.readable()) {
                packet.setData(buffer.toString(CharsetUtil.UTF_8));
            } else {
                packet.setData("");
            }
            break;
        }

        case EVENT: {
            ChannelBufferInputStream in = new ChannelBufferInputStream(buffer);
            Event event = jsonSupport.readValue(in, Event.class);
            packet.setName(event.getName());
            if (event.getArgs() != null) {
                packet.setArgs(event.getArgs());
            }
            break;
        }

        case JSON: {
            ChannelBufferInputStream in = new ChannelBufferInputStream(buffer);
            JsonObject obj = jsonSupport.readValue(in, JsonObject.class);
            if (obj != null) {
                packet.setData(obj.getObject());
            } else {
                in.reset();
                Object object = jsonSupport.readValue(in, Object.class);
                packet.setData(object);
            }
            break;
        }

        case CONNECT: {
            if (buffer.readable()) {
                packet.setQs(buffer.toString(CharsetUtil.UTF_8));
            }
            break;
        }

        case ACK: {
            if (!buffer.readable()) {
                break;
            }
            boolean validFormat = true;
            int plusIndex = -1;
            for (int i = buffer.readerIndex(); i < buffer.readerIndex() + buffer.readableBytes(); i++) {
                byte dataChar = buffer.getByte(i);
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
                packet.setAckId(parseLong(buffer));
                break;
            } else {
                packet.setAckId(parseLong(buffer, plusIndex));
                buffer.readerIndex(plusIndex+1);

                ChannelBufferInputStream in = new ChannelBufferInputStream(buffer);
                AckCallback<?> callback = ackManager.getCallback(uuid, packet.getAckId());
                AckArgs args = jsonSupport.readAckArgs(in, callback.getResultClass());
                packet.setArgs(args.getArgs());
            }
            break;
        }

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

    public Packet decodePacket(String string, UUID uuid) throws IOException {
        return decodePacket(ChannelBuffers.copiedBuffer(string, CharsetUtil.UTF_8), uuid);
    }

    public Packet decodePackets(ChannelBuffer buffer, UUID uuid) throws IOException {
        if (isCurrentDelimiter(buffer, buffer.readerIndex())) {
            buffer.readerIndex(buffer.readerIndex() + Packet.DELIMITER_BYTES.length);

            Integer len = extractLength(buffer);

            int startIndex = buffer.readerIndex();
            ChannelBuffer frame = buffer.slice(startIndex, len);
            Packet packet = decodePacket(frame, uuid);
            buffer.readerIndex(startIndex + len);
            return packet;
        } else {
            Packet packet = decodePacket(buffer, uuid);
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
        int delimiterIndex = ChannelBuffers.indexOf(buffer, buffer.readerIndex(), buffer.readerIndex() + buffer.readableBytes(), delimiterFinder);
        if (delimiterIndex == -1) {
            throw new DecoderException("Can't find tail delimiter");
        }

        long len = parseLong(buffer, delimiterIndex);
        buffer.readerIndex(delimiterIndex + Packet.DELIMITER_BYTES.length);
        return len;
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
