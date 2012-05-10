package com.corundumstudio.socketio;

import java.util.UUID;

public class AuthorizeMessage {

	private UUID sessionId;
	private String origin;
	private String msg;
	private String jsonpParam;

	public AuthorizeMessage(String msg, String jsonpParam, String origin, UUID sessionId) {
		this.origin = origin;
		this.msg = msg;
		this.jsonpParam = jsonpParam;
		this.sessionId = sessionId;
	}

	public UUID getSessionId() {
		return sessionId;
	}

	public String getOrigin() {
		return origin;
	}

	public String getMsg() {
		return msg;
	}

	public String getJsonpParam() {
		return jsonpParam;
	}

}
