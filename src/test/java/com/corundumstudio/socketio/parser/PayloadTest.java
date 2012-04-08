package com.corundumstudio.socketio.parser;

import java.io.IOException;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class PayloadTest {

	private final Decoder decoder = new Decoder(new ObjectMapper());

	//@Test
	public void testPayloadDecodePerf() throws IOException {
		long start = System.currentTimeMillis();
		for (int i = 0; i < 50000; i++) {
			decoder.decodePayload("\ufffd5\ufffd3:::5\ufffd7\ufffd3:::53d\ufffd3\ufffd0::\ufffd5\ufffd3:::5\ufffd7\ufffd3:::53d\ufffd3\ufffd0::\ufffd5\ufffd3:::5\ufffd7\ufffd3:::53d\ufffd3\ufffd0::\ufffd5\ufffd3:::5\ufffd7\ufffd3:::53d\ufffd3\ufffd0::\ufffd5\ufffd3:::5\ufffd7\ufffd3:::53d\ufffd3\ufffd0::");
		}
		long end = System.currentTimeMillis() - start;
		System.out.println(end + "ms");
		// 1143ms
	}
	
	@Test
	public void testPayloadDecode() throws IOException {
		List<Packet> payload = decoder.decodePayload("\ufffd5\ufffd3:::5\ufffd7\ufffd3:::53d\ufffd3\ufffd0::");
		Assert.assertEquals(3, payload.size());
		Packet msg1 = payload.get(0);
		Assert.assertEquals(PacketType.MESSAGE, msg1.getType());
		Assert.assertEquals("5", msg1.getData());
		Packet msg2 = payload.get(1);
		Assert.assertEquals(PacketType.MESSAGE, msg2.getType());
		Assert.assertEquals("53d", msg2.getData());
		Packet msg3 = payload.get(2);
		Assert.assertEquals(PacketType.DISCONNECT, msg3.getType());
	}
	
}
