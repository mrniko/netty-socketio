package com.corundumstudio.socketio.parser;

import java.io.IOException;
import java.util.Collections;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class EncoderJsonPacketTest {

	private Encoder encoder = new Encoder(new ObjectMapper());
	
	@Test
	public void testEncode() throws IOException {
		Packet packet = new Packet(PacketType.JSON);
		packet.setData("2");
		String result = encoder.encodePacket(packet);
		Assert.assertEquals("4:::\"2\"", result);
	}

	@Test
	public void testEncodeWithMessageIdAndAckData() throws IOException {
		Packet packet = new Packet(PacketType.JSON);
		packet.setId("1");
		packet.setAck("data");
		packet.setData(Collections.singletonMap("a", "b"));
		String result = encoder.encodePacket(packet);
		Assert.assertEquals("4:1+::{\"a\":\"b\"}", result);
	}
	
}
