package com.corundumstudio.socketio.listener;

import java.util.List;

import com.corundumstudio.socketio.AckRequest;
import com.corundumstudio.socketio.transport.NamespaceClient;

public interface EventInterceptor {

	void onEvent(NamespaceClient client, String eventName, List<Object> args, AckRequest ackRequest);
	
}
