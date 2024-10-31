/**
 * Copyright (c) 2012-2023 Nikita Koksharov
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

import com.corundumstudio.socketio.AckCallback;
import com.corundumstudio.socketio.ack.AckManager;
import com.corundumstudio.socketio.handler.ClientHead;
import com.corundumstudio.socketio.namespace.Namespace;
import io.netty.buffer.ByteBuf;
import io.netty.buffer.ByteBufInputStream;
import io.netty.buffer.Unpooled;
import io.netty.handler.codec.base64.Base64;
import io.netty.util.CharsetUtil;
import java.io.IOException;
import java.net.URLDecoder;
import java.util.LinkedList;
import java.util.Map;

public class PacketDecoder {

    private final UTF8CharsScanner utf8scanner = new UTF8CharsScanner();

    private final ByteBuf QUOTES = Unpooled.copiedBuffer("\"", CharsetUtil.UTF_8);

    private final JsonSupport jsonSupport;
    private final AckManager ackManager;

    public PacketDecoder(JsonSupport jsonSupport, AckManager ackManager) {
        this.jsonSupport = jsonSupport;
        this.ackManager = ackManager;
    }

    private boolean isStringPacket(ByteBuf content) {
        return content.getByte(content.readerIndex()) == 0x0;
    }

    // TODO optimize
    public ByteBuf preprocessJson(Integer jsonIndex, ByteBuf content) throws IOException {
        String packet = URLDecoder.decode(content.toString(CharsetUtil.UTF_8), CharsetUtil.UTF_8.name());

        if (jsonIndex != null) {
            /**
            * double escaping is required for escaped new lines because unescaping of new lines can be done safely on server-side
            * (c) socket.io.js
            *
            * @see https://github.com/Automattic/socket.io-client/blob/1.3.3/socket.io.js#L2682
            */
            packet = packet.replace("\\\\n", "\\n");

            // skip "d="
            packet = packet.substring(2);
        }

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

    private boolean hasLengthHeader(ByteBuf buffer) {
        for (int i = 0; i < Math.min(buffer.readableBytes(), 10); i++) {
            byte b = buffer.getByte(buffer.readerIndex() + i);
            if (b == (byte)':' && i > 0) {
                return true;
            }
            if (b > 57 || b < 48) {
                return false;
            }
        }
        return false;
    }

    public Packet decodePackets(ByteBuf buffer, ClientHead client) throws IOException {
        if (isStringPacket(buffer)) {
            // TODO refactor
            int maxLength = Math.min(buffer.readableBytes(), 10);
            int headEndIndex = buffer.bytesBefore(maxLength, (byte)-1);
            if (headEndIndex == -1) {
                headEndIndex = buffer.bytesBefore(maxLength, (byte)0x3f);
            }
            int len = (int) readLong(buffer, headEndIndex);

            ByteBuf frame = buffer.slice(buffer.readerIndex() + 1, len);
            // skip this frame
            buffer.readerIndex(buffer.readerIndex() + 1 + len);
            return decode(client, frame);
        } else if (hasLengthHeader(buffer)) {
            // TODO refactor
            int lengthEndIndex = buffer.bytesBefore((byte)':');
            int lenHeader = (int) readLong(buffer, lengthEndIndex);
            int len = utf8scanner.getActualLength(buffer, lenHeader);

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

        final int separatorPos = frame.bytesBefore((byte) 0x1E);
        final ByteBuf packetBuf;
        if (separatorPos > 0) {
            // Multiple packets in one, copy out the next packet to parse
            packetBuf = frame.copy(frame.readerIndex(), separatorPos);
            frame.skipBytes(separatorPos + 1);
        } else {
            packetBuf = frame;
        }

        PacketType type = readType(packetBuf);
        Packet packet = new Packet(type, head.getEngineIOVersion());

        if (type == PacketType.PING) {
            packet.setData(readString(packetBuf));
            return packet;
        }

        if (!packetBuf.isReadable()) {
            return packet;
        }

        PacketType innerType = readInnerType(packetBuf);
        packet.setSubType(innerType);

        parseHeader(packetBuf, packet, innerType);
        parseBody(head, packetBuf, packet);
        return packet;
    }

    private void parseHeader(ByteBuf frame, Packet packet, PacketType innerType) {
        int endIndex = frame.bytesBefore((byte)'[');
        if (endIndex <= 0) {
            return;
        }

        int attachmentsDividerIndex = frame.bytesBefore(endIndex, (byte)'-');
        boolean hasAttachments = attachmentsDividerIndex != -1;
        if (hasAttachments && (PacketType.BINARY_EVENT.equals(innerType)
                || PacketType.BINARY_ACK.equals(innerType))) {
            int attachments = (int) readLong(frame, attachmentsDividerIndex);
            packet.initAttachments(attachments);
            frame.readerIndex(frame.readerIndex() + 1);

            endIndex -= attachmentsDividerIndex + 1;
        }
        if (endIndex == 0) {
            return;
        }

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
            long ackId = readLong(frame, endIndex);
            packet.setAckId(ackId);
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
            if (frame.getByte(0) == 'b' && frame.getByte(1) == '4') {
                binaryPacket.addAttachment(Unpooled.copiedBuffer(frame));
            } else {
                ByteBuf attachBuf = Base64.encode(frame);
                binaryPacket.addAttachment(Unpooled.copiedBuffer(attachBuf));
                attachBuf.release();
            }
            frame.skipBytes(frame.readableBytes());

            if (binaryPacket.isAttachmentsLoaded()) {
                LinkedList<ByteBuf> slices = new LinkedList<ByteBuf>();
                ByteBuf source = binaryPacket.getDataSource();
                for (int i = 0; i < binaryPacket.getAttachments().size(); i++) {
                    ByteBuf attachment = binaryPacket.getAttachments().get(i);
                    ByteBuf scanValue = Unpooled.copiedBuffer("{\"_placeholder\":true,\"num\":" + i + "}", CharsetUtil.UTF_8);
                    int pos = PacketEncoder.find(source, scanValue);
                    if (pos == -1) {
                        scanValue = Unpooled.copiedBuffer("{\"num\":" + i + ",\"_placeholder\":true}", CharsetUtil.UTF_8);
                        pos = PacketEncoder.find(source, scanValue);
                        if (pos == -1) {
                            throw new IllegalStateException("Can't find attachment by index: " + i + " in packet source");
                        }
                    }

                    ByteBuf prefixBuf = source.slice(source.readerIndex(), pos - source.readerIndex());
                    slices.add(prefixBuf);
                    slices.add(QUOTES);
                    slices.add(attachment);
                    slices.add(QUOTES);

                    source.readerIndex(pos + scanValue.readableBytes());
                }
                slices.add(source.slice());

                ByteBuf compositeBuf = Unpooled.wrappedBuffer(slices.toArray(new ByteBuf[0]));
                parseBody(head, compositeBuf, binaryPacket);
                head.setLastBinaryPacket(null);
                return binaryPacket;
            }
        }
        return new Packet(PacketType.MESSAGE, head.getEngineIOVersion());
    }

    private void parseBody(ClientHead head, ByteBuf frame, Packet packet) throws IOException {
        if (packet.getType() == PacketType.MESSAGE) {
            if (packet.getSubType() == PacketType.CONNECT
                    || packet.getSubType() == PacketType.DISCONNECT) {
                packet.setNsp(readNamespace(frame, false));
                if (packet.getSubType() == PacketType.CONNECT && frame.readableBytes() > 0) {
                    final Object authArgs = jsonSupport.readValue(packet.getNsp(), new ByteBufInputStream(frame), Map.class);
                    packet.setData(authArgs);
                }
            }

            if (packet.hasAttachments() && !packet.isAttachmentsLoaded()) {
                packet.setDataSource(Unpooled.copiedBuffer(frame));
                frame.skipBytes(frame.readableBytes());
                head.setLastBinaryPacket(packet);
                return;
            }

            if (packet.getSubType() == PacketType.ACK
                    || packet.getSubType() == PacketType.BINARY_ACK) {
                AckCallback<?> callback = ackManager.getCallback(head.getSessionId(), packet.getAckId());
                if (callback != null) {
                    ByteBufInputStream in = new ByteBufInputStream(frame);
                    AckArgs args = jsonSupport.readAckArgs(in, callback);
                    packet.setData(args.getArgs());
                }else {
                    frame.clear();
                }
            }

            if (packet.getSubType() == PacketType.EVENT
                    || packet.getSubType() == PacketType.BINARY_EVENT) {
                ByteBufInputStream in = new ByteBufInputStream(frame);
                Event event = jsonSupport.readValue(packet.getNsp(), in, Event.class);
                packet.setName(event.getName());
                packet.setData(event.getArgs());
            }
        }
    }

    private String readNamespace(ByteBuf frame, final boolean defaultToAll) {

        /**
         * namespace post request with url queryString, like
         *  /message?a=1,
         *  /message,
         */
        ByteBuf buffer = frame.slice();


        int endIndex = buffer.bytesBefore((byte) '?');
        if (endIndex > 0) {
            String namespace = readString(buffer, endIndex);
            if(namespace.startsWith("/")) {
                frame.skipBytes(endIndex + 1);
                return namespace;
            }
        }
        endIndex = buffer.bytesBefore((byte) ',');
        if (endIndex > 0) {
            String namespace = readString(buffer, endIndex);
            if(namespace.startsWith("/")) {
                frame.skipBytes(endIndex + 1);
                return namespace;
            }
        }
        if (defaultToAll) {
            // skip this frame
            frame.skipBytes(frame.readableBytes());
            return readString(buffer);
        }
        return Namespace.DEFAULT_NAME;
    }

}
