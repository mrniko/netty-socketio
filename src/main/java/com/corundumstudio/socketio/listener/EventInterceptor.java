package com.corundumstudio.socketio.listener;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.transport.NamespaceClient;
import java.util.List;

public interface  EventInterceptor {
    void onEvent(NamespaceClient client, String eventName, List<Object> args, AckRequest ackRequest);
}
