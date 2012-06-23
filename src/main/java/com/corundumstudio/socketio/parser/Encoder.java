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
import java.util.Queue;

import org.codehaus.jackson.map.ObjectMapper;
import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferOutputStream;
import org.jboss.netty.buffer.ChannelBuffers;

public class Encoder {

    private final UTF8CharsScanner charsScanner = new UTF8CharsScanner();
    private final ObjectMapper objectMapper;

    public Encoder(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public ChannelBuffer encodeJsonP(String param, String msg) throws IOException {
        String message = "io.j[" + param + "]("
                + objectMapper.writeValueAsString(msg) + ");";
        return ChannelBuffers.wrappedBuffer(message.getBytes());
    }

    public ChannelBuffer encodePacket(Packet packet) throws IOException {
        ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
        ChannelBufferOutputStream out = new ChannelBufferOutputStream(buffer);
        encodePacket(packet, out);
        return buffer;
    }

    public ChannelBuffer encodePackets(Queue<Packet> packets) throws IOException {
        if (packets.size() == 1) {
            ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
            ChannelBufferOutputStream out = new ChannelBufferOutputStream(buffer);
            Packet packet = packets.poll();
            encodePacket(packet, out);
            return buffer;
        } else {
            ChannelBuffer buffer = ChannelBuffers.dynamicBuffer();
            while (true) {
                Packet packet = packets.poll();
                if (packet == null) {
                    break;
                }

                ChannelBuffer packetBuffer = ChannelBuffers.dynamicBuffer();
                ChannelBufferOutputStream out = new ChannelBufferOutputStream(packetBuffer);
                int len = encodePacket(packet, out);
                byte[] lenBytes = toChars(len);

                buffer.writeBytes(Packet.DELIMITER_BYTES);
                buffer.writeBytes(lenBytes);
                buffer.writeBytes(Packet.DELIMITER_BYTES);
                buffer.writeBytes(packetBuffer);
            }
            return buffer;
        }
    }

    private byte toChar(int number) {
        return (byte) (number ^ 0x30);
    }

    final static char[] DigitTens = {'0', '0', '0', '0', '0', '0', '0', '0', '0', '0', '1', '1', '1', '1',
            '1', '1', '1', '1', '1', '1', '2', '2', '2', '2', '2', '2', '2', '2', '2', '2', '3', '3', '3',
            '3', '3', '3', '3', '3', '3', '3', '4', '4', '4', '4', '4', '4', '4', '4', '4', '4', '5', '5',
            '5', '5', '5', '5', '5', '5', '5', '5', '6', '6', '6', '6', '6', '6', '6', '6', '6', '6', '7',
            '7', '7', '7', '7', '7', '7', '7', '7', '7', '8', '8', '8', '8', '8', '8', '8', '8', '8', '8',
            '9', '9', '9', '9', '9', '9', '9', '9', '9', '9',};

    final static char[] DigitOnes = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3',
            '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2',
            '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1',
            '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9', '0',
            '1', '2', '3', '4', '5', '6', '7', '8', '9', '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',
            '0', '1', '2', '3', '4', '5', '6', '7', '8', '9',};

    final static char[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'a', 'b', 'c', 'd', 'e',
            'f', 'g', 'h', 'i', 'j', 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't', 'u', 'v', 'w', 'x',
            'y', 'z'};

    final static int[] sizeTable = {9, 99, 999, 9999, 99999, 999999, 9999999, 99999999, 999999999,
            Integer.MAX_VALUE};

    // Requires positive x
    static int stringSize(long x) {
        for (int i = 0;; i++)
            if (x <= sizeTable[i])
                return i + 1;
    }

    static void getChars(long i, int index, byte[] buf) {
        long q, r;
        int charPos = index;
        byte sign = 0;

        if (i < 0) {
            sign = '-';
            i = -i;
        }

        // Generate two digits per iteration
        while (i >= 65536) {
            q = i / 100;
            // really: r = i - (q * 100);
            r = i - ((q << 6) + (q << 5) + (q << 2));
            i = q;
            buf[--charPos] = (byte) DigitOnes[(int)r];
            buf[--charPos] = (byte) DigitTens[(int)r];
        }

        // Fall thru to fast mode for smaller numbers
        // assert(i <= 65536, i);
        for (;;) {
            q = (i * 52429) >>> (16 + 3);
            r = i - ((q << 3) + (q << 1)); // r = i-(q*10) ...
            buf[--charPos] = (byte) digits[(int)r];
            i = q;
            if (i == 0)
                break;
        }
        if (sign != 0) {
            buf[--charPos] = sign;
        }
    }

    private byte[] toChars(long i) {
        int size = (i < 0) ? stringSize(-i) + 1 : stringSize(i);
        byte[] buf = new byte[size];
        getChars(i, size, buf);
        return buf;
    }

    private int encodePacket(Packet packet, ChannelBufferOutputStream out) throws IOException {
        ChannelBuffer buffer = out.buffer();
        int start = buffer.writerIndex();
        int type = packet.getType().getValue();
        buffer.writeByte(toChar(type));
        buffer.writeByte(Packet.SEPARATOR);

        Long id = packet.getId();
        String endpoint = packet.getEndpoint();
        Object ack = packet.getAck();

        if ("data".equals(ack)) {
            buffer.writeBytes(toChars(id));
            buffer.writeByte('+');
        } else {
            if (id != null) {
                buffer.writeBytes(toChars(id));
            }
        }
        buffer.writeByte(Packet.SEPARATOR);

        if (endpoint != null) {
            buffer.writeBytes(endpoint.getBytes());
        }

        switch (packet.getType()) {

        case MESSAGE:
            if (packet.getData() != null) {
                buffer.writeByte(Packet.SEPARATOR);
                byte[] data = packet.getData().toString().getBytes();
                buffer.writeBytes(data);
            }
            break;

        case EVENT:
            List<?> args = packet.getArgs();
            if (args.isEmpty()) {
                args = null;
            }
            buffer.writeByte(Packet.SEPARATOR);
            Event event = new Event(packet.getName(), args);
            objectMapper.writeValue(out, event);
            break;

        case JSON:
            buffer.writeByte(Packet.SEPARATOR);
            objectMapper.writeValue(out, packet.getData());
            break;

        case CONNECT:
            if (packet.getQs() != null) {
                buffer.writeByte(Packet.SEPARATOR);
                byte[] qsData = packet.getQs().toString().getBytes();
                buffer.writeBytes(qsData);
            }
            break;

        case ACK:
            if (packet.getAckId() != null || !packet.getArgs().isEmpty()) {
                buffer.writeByte(Packet.SEPARATOR);
            }
            if (packet.getAckId() != null) {
                byte[] ackIdData = toChars(packet.getAckId());
                buffer.writeBytes(ackIdData);
            }
            if (!packet.getArgs().isEmpty()) {
                buffer.writeByte('+');
                objectMapper.writeValue(out, packet.getArgs());
            }
            break;

        case ERROR:
            if (packet.getReason() != null || packet.getAdvice() != null) {
                buffer.writeByte(Packet.SEPARATOR);
            }
            if (packet.getReason() != null) {
                int reasonCode = packet.getReason().getValue();
                buffer.writeByte(toChar(reasonCode));
            }
            if (packet.getAdvice() != null) {
                int adviceCode = packet.getAdvice().getValue();
                buffer.writeByte('+');
                buffer.writeByte(toChar(adviceCode));
            }
            break;
        }
        return charsScanner.getLength(buffer, start);
    }

}
