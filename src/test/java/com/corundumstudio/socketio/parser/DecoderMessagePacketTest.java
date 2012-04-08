package com.corundumstudio.socketio.parser;

import java.io.IOException;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class DecoderMessagePacketTest {

	private final Decoder decoder = new Decoder(new ObjectMapper());
	
	@Test
	public void testDecode() throws IOException {
		Packet packet = decoder.decodePacket("3:::woot");
		Assert.assertEquals(PacketType.MESSAGE, packet.getType());
		Assert.assertEquals("woot", packet.getData());
	}

	@Test
	public void testDecodeWithIdAndEndpoint() throws IOException {
		Packet packet = decoder.decodePacket("3:5:/tobi");
		Assert.assertEquals(PacketType.MESSAGE, packet.getType());
		Assert.assertEquals("5", packet.getId());
		Assert.assertEquals(true, packet.getAck());
		Assert.assertEquals("/tobi", packet.getEndpoint());
	}

	
}

