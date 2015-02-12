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
package com.corundumstudio.socketio.protocol;

import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufAllocator;
import io.netty.buffer.ByteBufOutputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.base64.Base64Dialect;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;

import com.corundumstudio.socketio.Configuration;

public class PacketEncoder {

    private static final byte[] BINARY_HEADER = "b4".getBytes(CharsetUtil.UTF_8);
    private static final byte[] B64_DELIMITER = new byte[] {':'};
    private static final byte[] JSONP_HEAD = "___eio[".getBytes(CharsetUtil.UTF_8);
    private static final byte[] JSONP_START = "]('".getBytes(CharsetUtil.UTF_8);
    private static final byte[] JSONP_END = "');".getBytes(CharsetUtil.UTF_8);

    private final JsonSupport jsonSupport;
    private final Configuration configuration;

    public PacketEncoder(Configuration configuration, JsonSupport jsonSupport) {
        this.jsonSupport = jsonSupport;
        this.configuration = configuration;
    }

    public ByteBuf allocateBuffer(ByteBufAllocator allocator) {
        if (configuration.isPreferDirectBuffer()) {
            return allocator.ioBuffer();
        }

        return allocator.heapBuffer();
    }

    public void encodeJsonP(Integer jsonpIndex, Queue<Packet> packets, ByteBuf out, ByteBufAllocator allocator, int limit) throws IOException {
        boolean jsonpMode = jsonpIndex != null;

        ByteBuf buf = allocateBuffer(allocator);

        int i = 0;
        while (true) {
            Packet packet = packets.poll();
            if (packet == null || i == limit) {
                break;
            }

            ByteBuf packetBuf = allocateBuffer(allocator);
            encodePacket(packet, packetBuf, allocator, true);

            int packetSize = packetBuf.writerIndex();
            buf.writeBytes(toChars(packetSize));
            buf.writeBytes(B64_DELIMITER);
            buf.writeBytes(packetBuf);

            packetBuf.release();

            i++;

            for (ByteBuf attachment : packet.getAttachments()) {
                ByteBuf encodedBuf = Base64.encode(attachment, Base64Dialect.URL_SAFE);
                buf.writeBytes(toChars(encodedBuf.readableBytes() + 2));
                buf.writeBytes(B64_DELIMITER);
                buf.writeBytes(BINARY_HEADER);
                buf.writeBytes(encodedBuf);
            }
        }

        if (jsonpMode) {
            out.writeBytes(JSONP_HEAD);
            out.writeBytes(toChars(jsonpIndex));
            out.writeBytes(JSONP_START);
        }

        processUtf8(buf, out, jsonpMode);
        buf.release();

        if (jsonpMode) {
            out.writeBytes(JSONP_END);
        }
    }

    private void processUtf8(ByteBuf in, ByteBuf out, boolean jsonpMode) {
        while (in.isReadable()) {
            short value = (short) (in.readByte() & 0xFF);
            if (value >>> 7 == 0) {
                if (jsonpMode && (value == '\\' || value == '\'')) {
                    out.writeByte('\\');
                }
                out.writeByte(value);
            } else {
                out.writeByte(((value >>> 6) | 0xC0));
                out.writeByte(((value & 0x3F) | 0x80));
            }
        }
    }

    public void encodePackets(Queue<Packet> packets, ByteBuf buffer, ByteBufAllocator allocator, int limit) throws IOException {
        int i = 0;
        while (true) {
            Packet packet = packets.poll();
            if (packet == null || i == limit) {
                break;
            }
            encodePacket(packet, buffer, allocator, false);

            i++;

            for (ByteBuf attachment : packet.getAttachments()) {
                buffer.writeByte(1);
                buffer.writeBytes(longToBytes(attachment.readableBytes() + 1));
                buffer.writeByte(0xff);
                buffer.writeByte(4);
                buffer.writeBytes(attachment);
            }
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

    public static byte[] toChars(long i) {
        int size = (i < 0) ? stringSize(-i) + 1 : stringSize(i);
        byte[] buf = new byte[size];
        getChars(i, size, buf);
        return buf;
    }

    public static byte[] longToBytes(long number) {
        // TODO optimize
        int length = (int)(Math.log10(number)+1);
        byte[] res = new byte[length];
        int i = length;
        while (number > 0) {
            res[--i] = (byte) (number % 10);
            number = number / 10;
        }
        return res;
    }

    public void encodePacket(Packet packet, ByteBuf buffer, ByteBufAllocator allocator, boolean binary) throws IOException {
        ByteBuf buf = buffer;
        if (!binary) {
            buf = allocateBuffer(allocator);
        }
        byte type = toChar(packet.getType().getValue());
        buf.writeByte(type);

        try {
            switch (packet.getType()) {

                case PONG: {
                    buf.writeBytes(packet.getData().toString().getBytes(CharsetUtil.UTF_8));
                    break;
                }

                case OPEN: {
                    ByteBufOutputStream out = new ByteBufOutputStream(buf);
                    jsonSupport.writeValue(out, packet.getData());
                    break;
                }

                case MESSAGE: {

                    ByteBuf encBuf = null;

                    if (packet.getSubType() == PacketType.ERROR) {
                        encBuf = allocateBuffer(allocator);

                        ByteBufOutputStream out = new ByteBufOutputStream(encBuf);
                        jsonSupport.writeValue(out, packet.getData());
                    }

                    if (packet.getSubType() == PacketType.EVENT
                            || packet.getSubType() == PacketType.ACK) {

                        List<Object> values = new ArrayList<Object>();
                        if (packet.getSubType() == PacketType.EVENT) {
                            values.add(packet.getName());
                        }

                        encBuf = allocateBuffer(allocator);

                        List<Object> args = packet.getData();
                        values.addAll(args);
                        ByteBufOutputStream out = new ByteBufOutputStream(encBuf);
                        jsonSupport.writeValue(out, values);

                        if (!jsonSupport.getArrays().isEmpty()) {
                            packet.initAttachments(jsonSupport.getArrays().size());
                            for (byte[] array : jsonSupport.getArrays()) {
                                packet.addAttachment(Unpooled.wrappedBuffer(array));
                            }
                            packet.setSubType(PacketType.BINARY_EVENT);
                        }
                    }

                    byte subType = toChar(packet.getSubType().getValue());
                    buf.writeByte(subType);

                    if (packet.hasAttachments()) {
                        byte[] ackId = toChars(packet.getAttachments().size());
                        buf.writeBytes(ackId);
                        buf.writeByte('-');
                    }

                    if (packet.getSubType() == PacketType.CONNECT) {
                        if (!packet.getNsp().isEmpty()) {
                            buf.writeBytes(packet.getNsp().getBytes(CharsetUtil.UTF_8));
                        }
                    } else {
                        if (!packet.getNsp().isEmpty()) {
                            buf.writeBytes(packet.getNsp().getBytes(CharsetUtil.UTF_8));
                            buf.writeByte(',');
                        }
                    }

                    if (packet.getAckId() != null) {
                        byte[] ackId = toChars(packet.getAckId());
                        buf.writeBytes(ackId);
                    }

                    if (encBuf != null) {
                        buf.writeBytes(encBuf);
                        encBuf.release();
                    }

                    break;
                }
            }
        } finally {
            // we need to write a buffer in any case
            if (!binary) {
                buffer.writeByte(0);
                int length = buf.writerIndex();
                buffer.writeBytes(longToBytes(length));
                buffer.writeByte(0xff);
                buffer.writeBytes(buf);

                buf.release();
            }
        }
    }

    public static int find(ByteBuf buffer, ByteBuf searchValue) {
        for (int i = buffer.readerIndex(); i < buffer.readerIndex() + buffer.readableBytes(); i++) {
            if (isValueFound(buffer, i, searchValue)) {
                return i;
            }
        }
        return -1;
    }

    private static boolean isValueFound(ByteBuf buffer, int index, ByteBuf search) {
        for (int i = 0; i < search.readableBytes(); i++) {
            if (buffer.getByte(index + i) != search.getByte(i)) {
                return false;
            }
        }
        return true;
    }

}
