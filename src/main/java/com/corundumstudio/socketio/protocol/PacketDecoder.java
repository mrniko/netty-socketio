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
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.UUID;

import com.corundumstudio.socketio.AckCallback;
import com.corundumstudio.socketio.ack.AckManager;

public class PacketDecoder {

    private final JsonSupport jsonSupport;
    private final AckManager ackManager;

    public PacketDecoder(JsonSupport jsonSupport, AckManager ackManager) {
        this.jsonSupport = jsonSupport;
        this.ackManager = ackManager;
    }

    // TODO optimize
    public ByteBuf preprocessJson(Integer jsonIndex, ByteBuf content) throws IOException {
        String packet = URLDecoder.decode(content.toString(CharsetUtil.UTF_8), CharsetUtil.UTF_8.name());

        if (jsonIndex != null) {
            // skip "d="
            packet = packet.substring(2);
        }

        int splitIndex = packet.indexOf(":");
        String len = packet.substring(0, splitIndex);
        Integer length = Integer.valueOf(len);

        packet = packet.substring(splitIndex+1, splitIndex+length+1);
//        packet = new String(packet.getBytes(CharsetUtil.ISO_8859_1), CharsetUtil.UTF_8);

        return Unpooled.wrappedBuffer(packet.getBytes(CharsetUtil.UTF_8));
    }

    // fastest way to parse chars to int
    private long parseLong(ByteBuf chars, int length) {
        long result = 0;
        for (int i = chars.readerIndex(); i < chars.readerIndex() + length; i++) {
            int digit = ((int)chars.getByte(i) & 0xF);
            for (int j = 0; j < chars.readerIndex() + length-1-i; j++) {
                digit *= 10;
            }
            result += digit;
        }
        return result;
    }

    private PacketType readType(ByteBuf buffer) {
        int typeId = buffer.readByte() & 0xF;
        return PacketType.valueOf(typeId);
    }

    private PacketType readInnerType(ByteBuf buffer) {
        int typeId = buffer.readByte() & 0xF;
        return PacketType.valueOfInner(typeId);
    }

    @Deprecated
    public Packet decodePacket(String string, UUID uuid) throws IOException {
        ByteBuf buf = Unpooled.copiedBuffer(string, CharsetUtil.UTF_8);
        try {
            return null;
        } finally {
            buf.release();
        }
    }

    public Packet decodePackets(ByteBuf buffer, UUID uuid) throws IOException {
        boolean isString = buffer.getByte(buffer.readerIndex()) == 0x0;
        if (isString) {
            int headEndIndex = buffer.bytesBefore((byte)-1);
            int len = (int) parseLong(buffer, headEndIndex);

            buffer.readerIndex(buffer.readerIndex() + headEndIndex);

            ByteBuf frame = buffer.slice(buffer.readerIndex() + 1, len);
            // skip this frame
            buffer.readerIndex(buffer.readerIndex() + 1 + len);
            return decode(uuid, frame);
        }
        return decode(uuid, buffer);
    }

    private String readString(ByteBuf frame) {
        return readString(frame, frame.readableBytes());
    }

    private String readString(ByteBuf frame, int size) {
        byte[] bytes = new byte[size];
        frame.readBytes(bytes);
        return new String(bytes, CharsetUtil.UTF_8);
    }

    private Packet decode(UUID uuid, ByteBuf frame) throws IOException {
        PacketType type = readType(frame);
        Packet packet = new Packet(type);

        if (type == PacketType.PING) {
            packet.setData(readString(frame));
            return packet;
        }

        if (!frame.isReadable()) {
            return packet;
        }

        PacketType innerType = readInnerType(frame);
        packet.setSubType(innerType);

        int endIndex = frame.bytesBefore((byte)'[');
        if (endIndex > 0) {
            // TODO optimize
            String nspAckId = readString(frame, endIndex);
            if (nspAckId.contains(",")) {
                String[] parts = nspAckId.split(",");
                String nsp = parts[0];
                packet.setNsp(nsp);
                if (parts.length > 1) {
                    String ackId = parts[1];
                    packet.setAckId(Long.valueOf(ackId));
                }
            } else {
                packet.setAckId(Long.valueOf(nspAckId));
            }
        }

        if (packet.getType() == PacketType.MESSAGE) {
            if (packet.getSubType() == PacketType.CONNECT
                    || packet.getSubType() == PacketType.DISCONNECT) {
                packet.setNsp(readString(frame));
            }

            if (packet.getSubType() == PacketType.ACK) {
                ByteBufInputStream in = new ByteBufInputStream(frame);
                AckCallback<?> callback = ackManager.getCallback(uuid, packet.getAckId());
                AckArgs args = jsonSupport.readAckArgs(in, callback);
                packet.setData(args.getArgs());
            }

            if (packet.getSubType() == PacketType.EVENT
                    || packet.getSubType() == PacketType.BINARY_EVENT) {
                ByteBufInputStream in = new ByteBufInputStream(frame);
                Event event = jsonSupport.readValue(in, Event.class);
                packet.setName(event.getName());
                packet.setData(event.getArgs());
            }
        }
        return packet;
    }

}
