package com.corundumstudio.socketio.parser;

import java.io.IOException;

import org.codehaus.jackson.map.ObjectMapper;
import org.junit.Assert;
import org.junit.Test;

public class EncoderErrorPacketTest {

    private Encoder encoder = new Encoder(new ObjectMapper());

    @Test
    public void testEncode() throws IOException {
        Packet packet = new Packet(PacketType.ERROR);
        String result = encoder.encodePacket(packet);
        Assert.assertEquals("7::", result);
    }

    @Test
    public void testEncodeWithReason() throws IOException {
        Packet packet = new Packet(PacketType.ERROR);
        packet.setReason(ErrorReason.TRANSPORT_NOT_SUPPORTED);
        String result = encoder.encodePacket(packet);
        Assert.assertEquals("7:::0", result);
    }

    @Test
    public void testEncodeWithReasonAndAdvice() throws IOException {
        Packet packet = new Packet(PacketType.ERROR);
        packet.setReason(ErrorReason.UNAUTHORIZED);
        packet.setAdvice(ErrorAdvice.RECONNECT);
        String result = encoder.encodePacket(packet);
        Assert.assertEquals("7:::2+0", result);
    }

    @Test
    public void testEncodeWithEndpoint() throws IOException {
        Packet packet = new Packet(PacketType.ERROR);
        packet.setEndpoint("/woot");
        String result = encoder.encodePacket(packet);
        Assert.assertEquals("7::/woot", result);
    }
}
