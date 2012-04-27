package com.corundumstudio.socketio.parser;

import java.io.IOException;
import java.util.Arrays;

import org.codehaus.jackson.map.JsonMappingException;
import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class DecoderAckPacketTest {

    private final Decoder decoder = new Decoder(new ObjectMapper());

    @Test
    public void testDecode() throws IOException {
        Packet packet = decoder.decodePacket("6:::140");
        Assert.assertEquals(PacketType.ACK, packet.getType());
        Assert.assertEquals("140", packet.getAckId());
        Assert.assertTrue(packet.getArgs().isEmpty());
    }

    @Test
    public void testDecodeWithArgs() throws IOException {
        Packet packet = decoder.decodePacket("6:::12+[\"woot\",\"wa\"]");
        Assert.assertEquals(PacketType.ACK, packet.getType());
        Assert.assertEquals("12", packet.getAckId());
        Assert.assertEquals(Arrays.asList("woot", "wa"), packet.getArgs());
    }

    @Test(expected = JsonMappingException.class)
    public void testDecodeWithBadJson() throws IOException {
        decoder.decodePacket("6:::1+{\"++]");
    }

}
