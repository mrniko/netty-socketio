package com.corundumstudio.socketio.parser;

import java.io.IOException;
import java.util.Arrays;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class EncoderAckPacketTest {

	private Encoder encoder = new Encoder(new ObjectMapper());
	
	@Test
	public void testEncode() throws IOException {
		Packet packet = new Packet(PacketType.ACK);
		packet.setAckId("140");
		String result = encoder.encodePacket(packet);
		Assert.assertEquals("6:::140", result);
	}

	@Test
	public void testEncodeWithArgs() throws IOException {
		Packet packet = new Packet(PacketType.ACK);
		packet.setAckId("12");
		packet.setArgs(Arrays.asList("woot", "wa"));
		String result = encoder.encodePacket(packet);
		Assert.assertEquals("6:::12+[\"woot\",\"wa\"]", result);
	}

	
}
