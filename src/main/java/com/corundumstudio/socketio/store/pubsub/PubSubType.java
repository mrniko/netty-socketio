package com.corundumstudio.socketio.store.pubsub;

public enum PubSubType {
	
	CONNECT,
	DISCONNECT,
	JOIN,
	LEAVE,
	DISPATCH;
	
	@Override
	public String toString() {
		return name().toLowerCase();
	}
	
}
