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
package com.corundumstudio.socketio;

import java.io.IOException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.buffer.ChannelBufferIndexFinder;
import org.jboss.netty.buffer.ChannelBuffers;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.messages.PacketsMessage;
import com.corundumstudio.socketio.parser.Decoder;
import com.corundumstudio.socketio.parser.DecoderException;
import com.corundumstudio.socketio.parser.Packet;
import com.corundumstudio.socketio.parser.UTF8CharsScanner;

@Sharable
public class PacketHandler extends SimpleChannelUpstreamHandler {

	private final UTF8CharsScanner charsScanner = new UTF8CharsScanner();

    private final Logger log = LoggerFactory.getLogger(getClass());

    private final ChannelBufferIndexFinder delimiterFinder = new ChannelBufferIndexFinder() {
		@Override
		public boolean find(ChannelBuffer buffer, int guessedIndex) {
			return isCurrentDelimiter(buffer, guessedIndex);
		}
	};

    private final PacketListener packetListener;
    private final Decoder decoder;

    public PacketHandler(PacketListener packetListener, Decoder decoder) {
        super();
        this.packetListener = packetListener;
        this.decoder = decoder;
    }

    @Override
    public void messageReceived(ChannelHandlerContext ctx, MessageEvent e)
            throws Exception {
        Object msg = e.getMessage();
        if (msg instanceof PacketsMessage) {
            PacketsMessage message = (PacketsMessage) msg;
            ChannelBuffer content = message.getContent();

            if (log.isTraceEnabled()) {
                log.trace("In message: {} sessionId: {}", new Object[] {content.toString(CharsetUtil.UTF_8), message.getClient().getSessionId()});
            }
            while (content.readable()) {
                Packet packet = decodePacket(content);
                packetListener.onPacket(packet, message.getClient());
            }
        } else {
            ctx.sendUpstream(e);
        }
    }

    private Packet decodePacket(ChannelBuffer buffer) throws IOException {
        if (isCurrentDelimiter(buffer, buffer.readerIndex())) {
        	buffer.readerIndex(buffer.readerIndex() + Packet.DELIMITER_BYTES.length);

            Integer len = extractLength(buffer);

            ChannelBuffer frame = buffer.slice(buffer.readerIndex(), len);
            Packet packet = decoder.decodePacket(frame);
            buffer.readerIndex(buffer.readerIndex() + len);
            return packet;
        } else {
            Packet packet = decoder.decodePacket(buffer);
            buffer.readerIndex(buffer.readableBytes());
            return packet;
        }
    }

	private Integer extractLength(ChannelBuffer buffer) {
		Integer len = parseLengthHeader(buffer);

		// scan utf8 symbols if needed
		if (buffer.capacity() > buffer.readerIndex() + len
				&& !isCurrentDelimiter(buffer, buffer.readerIndex() + len)) {
			int index = charsScanner.findTailIndex(buffer, buffer.readerIndex(), buffer.capacity(), len);
			len = index - buffer.readerIndex();
		}
		return len;
	}

	private Integer parseLengthHeader(ChannelBuffer buffer) {
		int delimiterIndex = ChannelBuffers.indexOf(buffer, buffer.readerIndex(), buffer.capacity(), delimiterFinder);
		if (delimiterIndex == -1) {
			throw new DecoderException("Can't find tail delimiter");
		}

		byte[] digits = new byte[delimiterIndex - buffer.readerIndex()];;
		buffer.getBytes(buffer.readerIndex(), digits);
		buffer.readerIndex(buffer.readerIndex() + digits.length + Packet.DELIMITER_BYTES.length);
        return decoder.parseInt(digits);
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
