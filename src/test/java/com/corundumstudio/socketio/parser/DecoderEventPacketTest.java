package com.corundumstudio.socketio.parser;

import java.io.IOException;
import java.util.Map;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class DecoderEventPacketTest {

    private final Decoder decoder = new Decoder(new ObjectMapper());

    @Test
    public void testDecode() throws IOException {
        Packet packet = decoder.decodePacket("5:::{\"name\":\"woot\"}");
        Assert.assertEquals(PacketType.EVENT, packet.getType());
        Assert.assertEquals("woot", packet.getName());
    }

    @Test
    public void testDecodeWithMessageIdAndAck() throws IOException {
        Packet packet = decoder.decodePacket("5:1+::{\"name\":\"tobi\"}");
        Assert.assertEquals(PacketType.EVENT, packet.getType());
        Assert.assertEquals("1", packet.getId());
        Assert.assertEquals("data", packet.getAck());
        Assert.assertEquals("tobi", packet.getName());
    }

    @Test
    public void testDecodeWithData() throws IOException {
        Packet packet = decoder.decodePacket("5:::{\"name\":\"edwald\",\"args\":[{\"a\": \"b\"},2,\"3\"]}");
        Assert.assertEquals(PacketType.EVENT, packet.getType());
        Assert.assertEquals("edwald", packet.getName());
        Assert.assertEquals(3, packet.getArgs().size());
        Map obj = (Map) packet.getArgs().get(0);
        Assert.assertEquals("b", obj.get("a"));
        Assert.assertEquals(2, packet.getArgs().get(1));
        Assert.assertEquals("3", packet.getArgs().get(2));
    }

}
