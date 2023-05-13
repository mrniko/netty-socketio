package com.corundumstudio.socketio.protocol;

import java.util.UUID;

public class ConnPacket {

    private final UUID sid;

    public ConnPacket(UUID sid) {
        this.sid = sid;
    }

    public UUID getSid() {
        return sid;
    }
}
