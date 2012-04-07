package com.corundumstudio.socketio.parser;

import java.io.IOException;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class DecoderJsonPacketTest {

	private final Decoder decoder = new Decoder(new ObjectMapper());
	
	@Test
	public void testDecode() throws IOException {
		Packet packet = decoder.decodePacket("4:::\"2\"");
		Assert.assertEquals(PacketType.JSON, packet.getType());
		Assert.assertEquals("2", packet.getData());
	}

	@Test
	public void testDecodeWithMessageIdAndAckData() throws IOException {
		Packet packet = decoder.decodePacket("4:1+::{\"a\":\"b\"}");
		Assert.assertEquals(PacketType.JSON, packet.getType());
		Assert.assertEquals("1", packet.getId());
		Assert.assertEquals("data", packet.getAck());
		Map obj = (Map) packet.getData();
		Assert.assertEquals("b", obj.get("a"));
		Assert.assertEquals(1, obj.size());
	}
	
}
