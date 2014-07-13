package com.corundumstudio.socketio.handler;

import java.util.UUID;

import com.corundumstudio.socketio.DefaultSessionID;
import com.corundumstudio.socketio.SessionID;

public class DefaultSessionIDFactory implements SessionIDFactory {

    @Override
    public SessionID generateNew() {
        return new DefaultSessionID(UUID.randomUUID());
    }

    @Override
    public SessionID fromString(String string) {
        return new DefaultSessionID(UUID.fromString(string));
    }

}