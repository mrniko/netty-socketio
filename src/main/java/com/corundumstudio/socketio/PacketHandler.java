package com.corundumstudio.socketio;

import java.io.IOException;

import org.jboss.netty.buffer.ChannelBuffer;
import org.jboss.netty.channel.ChannelHandler.Sharable;
import org.jboss.netty.channel.ChannelHandlerContext;
import org.jboss.netty.channel.MessageEvent;
import org.jboss.netty.channel.SimpleChannelUpstreamHandler;
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

	private Packet decode(ChannelBuffer buffer) throws IOException {
		if (isCurrentDelimiter(buffer, buffer.readerIndex())) {
			StringBuilder length = new StringBuilder(4);
            for (int i = buffer.readerIndex() + Packet.DELIMITER_BYTES.length; i < buffer.readerIndex() + buffer.readableBytes(); i++) {
                if (isCurrentDelimiter(buffer, i)) {
                    break;
                } else {
                    length.append((char)buffer.getUnsignedByte(i));
                }
            }
            Integer len = Integer.valueOf(length.toString());

            int startIndex = buffer.readerIndex() + Packet.DELIMITER_BYTES.length + length.length() + Packet.DELIMITER_BYTES.length;
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

	private boolean isCurrentDelimiter(ChannelBuffer buffer, int index) {
		for (int i = 0; i < Packet.DELIMITER_BYTES.length; i++) {
			if (buffer.getByte(index + i) != Packet.DELIMITER_BYTES[i]) {
				return false;
			}
		}
		return true;
	}

}
