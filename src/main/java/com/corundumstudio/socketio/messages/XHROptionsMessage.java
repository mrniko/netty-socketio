package com.corundumstudio.socketio.messages;

import java.util.UUID;

public class XHROptionsMessage extends XHROutMessage {

    public XHROptionsMessage(String origin, UUID sessionId) {
        super(origin, sessionId);
    }

}
