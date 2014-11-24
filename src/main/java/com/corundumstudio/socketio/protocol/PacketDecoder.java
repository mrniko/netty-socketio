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
import io.netty.handler.codec.base64.Base64;
import io.netty.handler.codec.base64.Base64Dialect;
import io.netty.util.CharsetUtil;

import java.io.IOException;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.UUID;

import com.corundumstudio.socketio.AckCallback;
import com.corundumstudio.socketio.ack.AckManager;
import com.corundumstudio.socketio.handler.ClientHead;
import com.corundumstudio.socketio.namespace.NamespacesHub;

public class PacketDecoder {

    private final ByteBuf QUOTES = Unpooled.copiedBuffer("\"", CharsetUtil.UTF_8);

    private final JsonSupport jsonSupport;
    private final AckManager ackManager;
    private final NamespacesHub nspHub;

    public PacketDecoder(JsonSupport jsonSupport, NamespacesHub nspHub, AckManager ackManager) {
        this.jsonSupport = jsonSupport;
        this.ackManager = ackManager;
        this.nspHub = nspHub;
    }

    // TODO optimize
    public ByteBuf preprocessJson(Integer jsonIndex, ByteBuf content) throws IOException {
        String packet = URLDecoder.decode(content.toString(CharsetUtil.UTF_8), CharsetUtil.UTF_8.name());

        int startPos = 0;
        if (jsonIndex != null) {
            // skip "d="
            startPos = 2;
        }

        int splitIndex = packet.indexOf(":");
        String len = packet.substring(startPos, splitIndex);
        Integer length = Integer.valueOf(len);

        packet = packet.substring(splitIndex+1, splitIndex+length+1);
        packet = new String(packet.getBytes(CharsetUtil.ISO_8859_1), CharsetUtil.UTF_8);

        return Unpooled.wrappedBuffer(packet.getBytes(CharsetUtil.UTF_8));
    }

    // fastest way to parse chars to int
    private long readLong(ByteBuf chars, int length) {
        long result = 0;
        for (int i = chars.readerIndex(); i < chars.readerIndex() + length; i++) {
            int digit = ((int)chars.getByte(i) & 0xF);
            for (int j = 0; j < chars.readerIndex() + length-1-i; j++) {
                digit *= 10;
            }
            result += digit;
        }
        chars.readerIndex(chars.readerIndex() + length);
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

    public Packet decodePackets(ByteBuf buffer, ClientHead client) throws IOException {
        boolean isString = buffer.getByte(buffer.readerIndex()) == 0x0;
        if (isString) {
            int headEndIndex = buffer.bytesBefore((byte)-1);
            int len = (int) readLong(buffer, headEndIndex);

            ByteBuf frame = buffer.slice(buffer.readerIndex() + 1, len);
            // skip this frame
            buffer.readerIndex(buffer.readerIndex() + 1 + len);
            return decode(client, frame);
        }
        return decode(client, buffer);
    }

    private String readString(ByteBuf frame) {
        return readString(frame, frame.readableBytes());
    }

    private String readString(ByteBuf frame, int size) {
        byte[] bytes = new byte[size];
        frame.readBytes(bytes);
        return new String(bytes, CharsetUtil.UTF_8);
    }

    private Packet decode(ClientHead head, ByteBuf frame) throws IOException {
        if ((frame.getByte(0) == 'b' && frame.getByte(1) == '4')
                || frame.getByte(0) == 4 || frame.getByte(0) == 1) {
            return parseBinary(head, frame);
        }
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

        parseHeader(frame, packet, innerType);
        parseBody(head, frame, packet);
        return packet;
    }

    private void parseHeader(ByteBuf frame, Packet packet, PacketType innerType) {
        int endIndex = frame.bytesBefore((byte)'[');
        if (endIndex > 0) {
            // TODO optimize
            boolean hasNsp = frame.bytesBefore(endIndex, (byte)',') != -1;
            if (hasNsp) {
                String nspAckId = readString(frame, endIndex);
                String[] parts = nspAckId.split(",");
                String nsp = parts[0];
                packet.setNsp(nsp);
                if (parts.length > 1) {
                    String ackId = parts[1];
                    packet.setAckId(Long.valueOf(ackId));
                }
            } else {
                boolean hasAttachments = frame.bytesBefore(endIndex, (byte)'-') != -1;
                if (hasAttachments && PacketType.BINARY_EVENT.equals(innerType)) {
                    int attachments = (int) readLong(frame, endIndex-1);
                    packet.initAttachments(attachments);
                    frame.readerIndex(frame.readerIndex() + 1);
                } else {
                    long ackId = readLong(frame, endIndex);
                    packet.setAckId(ackId);
                }
            }
        }
    }

    private Packet parseBinary(ClientHead head, ByteBuf frame) throws IOException {
        if (frame.getByte(0) == 1) {
            frame.readByte();
            int headEndIndex = frame.bytesBefore((byte)-1);
            int len = (int) readLong(frame, headEndIndex);
            ByteBuf oldFrame = frame;
            frame = frame.slice(oldFrame.readerIndex() + 1, len);
            oldFrame.readerIndex(oldFrame.readerIndex() + 1 + len);
        }

        if (frame.getByte(0) == 'b' && frame.getByte(1) == '4') {
            frame.readShort();
        } else if (frame.getByte(0) == 4) {
            frame.readByte();
        }

        Packet binaryPacket = head.getLastBinaryPacket();
        if (binaryPacket != null) {
            ByteBuf attachBuf;
            if (frame.getByte(0) == 'b' && frame.getByte(1) == '4') {
                attachBuf = frame;
            } else {
                attachBuf = Base64.encode(frame);
            }
            binaryPacket.addAttachment(Unpooled.copiedBuffer(attachBuf));
            frame.readerIndex(frame.readerIndex() + frame.readableBytes());

            if (binaryPacket.isAttachmentsLoaded()) {
                LinkedList<ByteBuf> slices = new LinkedList<ByteBuf>();
                ByteBuf source = binaryPacket.getDataSource();
                for (int i = 0; i < binaryPacket.getAttachments().size(); i++) {
                    ByteBuf attachment = binaryPacket.getAttachments().get(i);
                    ByteBuf scanValue = Unpooled.copiedBuffer("{\"_placeholder\":true,\"num\":" + i + "}", CharsetUtil.UTF_8);
                    int pos = PacketEncoder.find(source, scanValue);
                    if (pos == -1) {
                        throw new IllegalStateException("Can't find attachment by index: " + i + " in packet source");
                    }

                    ByteBuf prefixBuf = source.slice(source.readerIndex(), pos - source.readerIndex());
                    slices.add(prefixBuf);
                    slices.add(QUOTES);
                    slices.add(attachment);
                    slices.add(QUOTES);

                    source.readerIndex(pos + scanValue.readableBytes());
                }
                slices.add(source.slice());

                ByteBuf compositeBuf = Unpooled.wrappedBuffer(slices.toArray(new ByteBuf[slices.size()]));
                parseBody(head, compositeBuf, binaryPacket);
                head.setLastBinaryPacket(null);
                return binaryPacket;
            }
        }
        return new Packet(PacketType.MESSAGE);
    }

    private void parseBody(ClientHead head, ByteBuf frame, Packet packet) throws IOException {
        if (packet.getType() == PacketType.MESSAGE) {
            if (packet.getSubType() == PacketType.CONNECT
                    || packet.getSubType() == PacketType.DISCONNECT) {
                packet.setNsp(readString(frame));
            }

            if (packet.getSubType() == PacketType.ACK) {
                ByteBufInputStream in = new ByteBufInputStream(frame);
                AckCallback<?> callback = ackManager.getCallback(head.getSessionId(), packet.getAckId());
                AckArgs args = jsonSupport.readAckArgs(in, callback);
                packet.setData(args.getArgs());
            }

            if (packet.getSubType() == PacketType.EVENT
                    || packet.getSubType() == PacketType.BINARY_EVENT) {
                if (packet.hasAttachments() && !packet.isAttachmentsLoaded()) {
                    packet.setDataSource(Unpooled.copiedBuffer(frame));
                    frame.readerIndex(frame.readableBytes());
                    head.setLastBinaryPacket(packet);
                } else {
                    ByteBufInputStream in = new ByteBufInputStream(frame);
                    Event event = jsonSupport.readValue(packet.getNsp(), in, Event.class);
                    packet.setName(event.getName());
                    packet.setData(event.getArgs());
                }
            }
        }
    }

}
