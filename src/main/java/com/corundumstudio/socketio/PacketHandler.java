package com.corundumstudio.socketio;

import java.io.IOException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.util.CharsetUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.corundumstudio.socketio.messages.PacketsMessage;
import com.corundumstudio.socketio.parser.Decoder;
import com.corundumstudio.socketio.parser.Packet;

@Sharable
public class PacketHandler extends SimpleChannelUpstreamHandler {

	private final Logger log = LoggerFactory.getLogger(getClass());

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
				Packet packet = decode(content);
				packetListener.onPacket(packet, message.getClient());
			}
		} else {
			ctx.sendUpstream(e);
		}
	}

	// TODO use ForkJoin
	private Packet decode(ChannelBuffer buffer) throws IOException {
		char delimiter = getChar(buffer, buffer.readerIndex());
		if (delimiter == Packet.DELIMITER) {
			StringBuilder length = new StringBuilder(4);
            for (int i = buffer.readerIndex() + 2 + 1; i < buffer.readerIndex() + buffer.readableBytes(); i++) {
                if (getChar(buffer, i) == Packet.DELIMITER) {
                    break;
                } else {
                    length.append((char)buffer.getUnsignedByte(i));
                }
            }
            Integer len = Integer.valueOf(length.toString());

            int startIndex = buffer.readerIndex() + 3 + length.length() + 3;
	        ChannelBuffer frame = buffer.slice(startIndex, len);
	        Packet packet = decoder.decodePacket(frame.toString(CharsetUtil.UTF_8));
	        buffer.readerIndex(startIndex + len);
	        return packet;
		} else {
	        Packet packet = decoder.decodePacket(buffer.toString(CharsetUtil.UTF_8));
	        buffer.readerIndex(buffer.readableBytes());
	        return packet;
		}
	}

	// TODO refactor it
	private char getChar(ChannelBuffer buffer, int index) {
		byte[] bytes = {buffer.getByte(index), buffer.getByte(index + 1)};
		return new String(bytes).charAt(0);
	}

}
