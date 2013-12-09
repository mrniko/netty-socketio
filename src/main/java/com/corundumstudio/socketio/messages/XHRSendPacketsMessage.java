package com.corundumstudio.socketio.messages;

import java.util.Queue;
import java.util.UUID;

import com.corundumstudio.socketio.parser.Packet;

public class XHRSendPacketsMessage extends HttpMessage {

    private final Queue<Packet> packetQueue;

    public XHRSendPacketsMessage(UUID sessionId, String origin, Queue<Packet> packetQueue) {
        super(origin, sessionId);
        this.packetQueue = packetQueue;
    }

    public Queue<Packet> getPacketQueue() {
        return packetQueue;
    }

}
