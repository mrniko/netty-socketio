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
        	buffer.readerIndex(buffer.readerIndex() + Packet.DELIMITER_BYTES.length);

            Integer len = parseLength(buffer);

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

	private Integer parseLength(ChannelBuffer buffer) {
		byte[] digits = null;
		for (int i = buffer.readerIndex(); i < buffer.readerIndex() + buffer.readableBytes(); i++) {
		    if (isCurrentDelimiter(buffer, i)) {
		    	digits = new byte[i - buffer.readerIndex()];
		    	buffer.getBytes(buffer.readerIndex(), digits);
		        break;
		    }
		}
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
