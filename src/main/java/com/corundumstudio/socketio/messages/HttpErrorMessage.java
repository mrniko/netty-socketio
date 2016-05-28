package com.corundumstudio.socketio.messages;

import java.util.Map;

public class HttpErrorMessage extends HttpMessage {

    private final Map<String, Object> data;

    public HttpErrorMessage(Map<String, Object> data) {
        super(null, null);
        this.data = data;
    }

    public Map<String, Object> getData() {
        return data;
    }
    
}
