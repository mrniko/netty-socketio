package com.corundumstudio.socketio.parser;

import java.io.IOException;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class EncoderMessagePacketTest {

    private Encoder encoder = new Encoder(new ObjectMapper());

    @Test
    public void testEncode() throws IOException {
        Packet packet = new Packet(PacketType.MESSAGE);
        packet.setData("woot");
        String result = encoder.encodePacket(packet);
        Assert.assertEquals("3:::woot", result);
    }

    @Test
    public void testEncodeWithIdAndEndpoint() throws IOException {
        Packet packet = new Packet(PacketType.MESSAGE);
        packet.setId("5");
        packet.setAck(true);
        packet.setEndpoint("/tobi");
        String result = encoder.encodePacket(packet);
        Assert.assertEquals("3:5:/tobi", result);
    }

}
