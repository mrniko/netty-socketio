package com.corundumstudio.socketio.parser;

import java.io.IOException;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class DecoderConnectionPacketTest {

    private final Decoder decoder = new Decoder(new ObjectMapper());

    @Test
    public void testDecodeHeartbeat() throws IOException {
        Packet packet = decoder.decodePacket("2:::");
        Assert.assertEquals(PacketType.HEARTBEAT, packet.getType());
    }

    @Test
    public void testDecode() throws IOException {
        Packet packet = decoder.decodePacket("1::/tobi");
        Assert.assertEquals(PacketType.CONNECT, packet.getType());
        Assert.assertEquals("/tobi", packet.getEndpoint());
    }

    @Test
    public void testDecodeWithQueryString() throws IOException {
        Packet packet = decoder.decodePacket("1::/test:?test=1");
        Assert.assertEquals(PacketType.CONNECT, packet.getType());
        Assert.assertEquals("/test", packet.getEndpoint());
        Assert.assertEquals("?test=1", packet.getQs());
    }

    @Test
    public void testDecodeDisconnection() throws IOException {
        Packet packet = decoder.decodePacket("0::/woot");
        Assert.assertEquals(PacketType.DISCONNECT, packet.getType());
        Assert.assertEquals("/woot", packet.getEndpoint());
    }

}
