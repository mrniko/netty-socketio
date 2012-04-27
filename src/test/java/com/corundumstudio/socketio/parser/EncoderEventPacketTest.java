package com.corundumstudio.socketio.parser;

import java.io.IOException;
import java.util.Arrays;
import java.util.Collections;

import org.codehaus.jackson.map.ObjectMapper;
import org.codehaus.jackson.map.annotate.JsonSerialize;
import org.junit.Assert;
import org.junit.Test;

public class EncoderEventPacketTest {

    private Encoder encoder;

    public EncoderEventPacketTest() {
        ObjectMapper om = new ObjectMapper();
        om.setSerializationInclusion(JsonSerialize.Inclusion.NON_NULL);
        encoder = new Encoder(om);
    }

    @Test
    public void testEncode() throws IOException {
        Packet packet = new Packet(PacketType.EVENT);
        packet.setName("woot");
        String result = encoder.encodePacket(packet);
        Assert.assertEquals("5:::{\"name\":\"woot\"}", result);
    }

    @Test
    public void testEncodeWithMessageIdAndAck() throws IOException {
        Packet packet = new Packet(PacketType.EVENT);
        packet.setId("1");
        packet.setAck("data");
        packet.setName("tobi");
        String result = encoder.encodePacket(packet);
        Assert.assertEquals("5:1+::{\"name\":\"tobi\"}", result);
    }

    @Test
    public void testEncodeWithData() throws IOException {
        Packet packet = new Packet(PacketType.EVENT);
        packet.setName("edwald");
        packet.setArgs(Arrays.asList(Collections.singletonMap("a", "b"), 2, "3"));
        String result = encoder.encodePacket(packet);
        Assert.assertEquals("5:::{\"name\":\"edwald\",\"args\":[{\"a\":\"b\"},2,\"3\"]}", result);
    }

}
