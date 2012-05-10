package com.corundumstudio.socketio;

import com.corundumstudio.socketio.parser.Packet;

public class ErrorMessage {

    public enum Type {XHR, WEBSOCKET};

    private Type type;
    private Packet packet;
    private String origin;

    public ErrorMessage(Type type, Packet packet) {
        super();
        this.type = type;
        this.packet = packet;
    }

    public ErrorMessage(Type type, Packet packet, String origin) {
        super();
        this.type = type;
        this.packet = packet;
        this.origin = origin;
    }

    public String getOrigin() {
        return origin;
    }

    public Type getType() {
        return type;
    }

    public Packet getPacket() {
        return packet;
    }

}
