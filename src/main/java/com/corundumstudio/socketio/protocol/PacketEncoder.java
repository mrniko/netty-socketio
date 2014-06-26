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
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Queue;
import java.util.regex.Pattern;

import com.corundumstudio.socketio.Configuration;

public class PacketEncoder {

    private static final Pattern QUOTES_PATTERN = Pattern.compile("\"", Pattern.LITERAL);
    private static final byte[] JSONP_DELIMITER = new byte[] {':'};
    private static final byte[] JSONP_HEAD = "___eio[".getBytes(CharsetUtil.UTF_8);
    private static final byte[] JSONP_START = "](\"".getBytes(CharsetUtil.UTF_8);
    private static final byte[] JSONP_END = "\");".getBytes(CharsetUtil.UTF_8);

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
        int i = 0;
        ByteBuf buf = allocateBuffer(allocator);

        while (true) {
            Packet packet = packets.poll();
            if (packet == null || i == limit) {
                break;
            }

            ByteBuf packetBuf = allocateBuffer(allocator);
            encodePacket(packet, packetBuf, allocator, true, true);
            // scan for \\\"
            int count = count(packetBuf, Unpooled.copiedBuffer("\\\"", CharsetUtil.UTF_8));
            int packetSize = packetBuf.writerIndex() - count;

            buf.writeBytes(toChars(packetSize));
            buf.writeBytes(JSONP_DELIMITER);
            buf.writeBytes(packetBuf);

            i++;
        }

        out.writeBytes(JSONP_HEAD);
        out.writeBytes(toChars(jsonpIndex));
        out.writeBytes(JSONP_START);

        // TODO optimize
        String packet = buf.toString(CharsetUtil.UTF_8);
        packet = QUOTES_PATTERN.matcher(packet).replaceAll("\\\\\"");
        packet = new String(packet.getBytes(CharsetUtil.UTF_8), CharsetUtil.ISO_8859_1);

        out.writeBytes(packet.getBytes(CharsetUtil.UTF_8));
        out.writeBytes(JSONP_END);
    }

    public void encodePackets(Queue<Packet> packets, ByteBuf buffer, ByteBufAllocator allocator, int limit) throws IOException {
        int i = 0;
        while (true) {
            Packet packet = packets.poll();
            if (packet == null || i == limit) {
                break;
            }
            encodePacket(packet, buffer, allocator, false, false);
            i++;
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

    public void encodePacket(Packet packet, ByteBuf buffer, ByteBufAllocator allocator, boolean binary, boolean jsonp) throws IOException {
        ByteBuf buf = buffer;
        if (!binary) {
            buf = allocateBuffer(allocator);
        }
        byte type = toChar(packet.getType().getValue());
        buf.writeByte(type);

        switch (packet.getType()) {

            case PONG: {
                buf.writeBytes(packet.getData().toString().getBytes(CharsetUtil.UTF_8));
                break;
            }

            case OPEN: {
                ByteBufOutputStream out = new ByteBufOutputStream(buf);
                if (jsonp) {
                    jsonSupport.writeJsonValue(out, packet.getData());
                } else {
                    jsonSupport.writeValue(out, packet.getData());
                }
                break;
            }

            case MESSAGE: {
                byte subType = toChar(packet.getSubType().getValue());
                buf.writeByte(subType);

                if (packet.getSubType() == PacketType.CONNECT) {
                    if (!packet.getNsp().isEmpty()) {
                        buf.writeBytes(packet.getNsp().getBytes(CharsetUtil.UTF_8));
                    }
                } else {
                    if (!packet.getNsp().isEmpty()) {
                        buf.writeBytes(packet.getNsp().getBytes(CharsetUtil.UTF_8));
                        buf.writeBytes(new byte[] {','});
                    }
                }

                if (packet.getAckId() != null) {
                    byte[] ackId = toChars(packet.getAckId());
                    buf.writeBytes(ackId);
                }

                List<Object> values = new ArrayList<Object>();

                if (packet.getSubType() == PacketType.EVENT
                        || packet.getSubType() == PacketType.ERROR) {
                    values.add(packet.getName());
                }

                if (packet.getSubType() == PacketType.EVENT
                        || packet.getSubType() == PacketType.ACK
                            || packet.getSubType() == PacketType.ERROR) {
                    List<Object> args = packet.getData();
                    values.addAll(args);
                    ByteBufOutputStream out = new ByteBufOutputStream(buf);
                    if (jsonp) {
                        jsonSupport.writeJsonValue(out, values);
                    } else {
                        jsonSupport.writeValue(out, values);
                    }
                }
                break;
            }
        }

        if (!binary) {
            buffer.writeByte(0);
            int length = buf.writerIndex();
            buffer.writeBytes(longToBytes(length));
            buffer.writeByte(0xff);
            buffer.writeBytes(buf);
        }
    }

    private int count(ByteBuf buffer, ByteBuf searchValue) {
        int count = 0;
        for (int i = 0; i < buffer.readableBytes(); i++) {
            if (isValueFound(buffer, i, searchValue)) {
                count++;
            }
        }
        return count;
    }

    private boolean isValueFound(ByteBuf buffer, int index, ByteBuf search) {
        for (int i = 0; i < search.readableBytes(); i++) {
            if (buffer.getByte(index + i) != search.getByte(i)) {
                return false;
            }
        }
        return true;
    }

}
