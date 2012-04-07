package com.corundumstudio.socketio.parser;

import java.io.IOException;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class DecoderErrorPacketTest {

	private final Decoder decoder = new Decoder(new ObjectMapper());
	
	@Test
	public void testDecode() throws IOException {
		Packet packet = decoder.decodePacket("7:::");
		Assert.assertEquals(PacketType.ERROR, packet.getType());
	}
	
	@Test
	public void testDecodeWithReason() throws IOException {
		Packet packet = decoder.decodePacket("7:::0");
		Assert.assertEquals(PacketType.ERROR, packet.getType());
		Assert.assertEquals(ErrorReason.TRANSPORT_NOT_SUPPORTED, packet.getReason());
	}

	@Test
	public void testDecodeWithReasonAndAdvice() throws IOException {
		Packet packet = decoder.decodePacket("7:::2+0");
		Assert.assertEquals(PacketType.ERROR, packet.getType());
		Assert.assertEquals(ErrorReason.UNAUTHORIZED, packet.getReason());
		Assert.assertEquals(ErrorAdvice.RECONNECT, packet.getAdvice());
	}

	@Test
	public void testDecodeWithEndpoint() throws IOException {
		Packet packet = decoder.decodePacket("7::/woot");
		Assert.assertEquals(PacketType.ERROR, packet.getType());
		Assert.assertEquals("/woot", packet.getEndpoint());
	}
	
	
}
