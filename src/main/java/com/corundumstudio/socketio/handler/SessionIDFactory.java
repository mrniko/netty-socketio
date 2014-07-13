package com.corundumstudio.socketio.handler;

import com.corundumstudio.socketio.SessionID;

public interface SessionIDFactory {

    SessionID generateNew();

    SessionID fromString(String string);
    
}
