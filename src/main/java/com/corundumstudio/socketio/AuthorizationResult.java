package com.corundumstudio.socketio;

import java.util.Collections;
import java.util.Map;

public class AuthorizationResult {

	public static final AuthorizationResult SUCCESSFUL_AUTHORIZATION = new AuthorizationResult(true);
	public static final AuthorizationResult FAILED_AUTHORIZATION = new AuthorizationResult(false);
	private final boolean isAuthorized;
	private final Map<String, Object> storeParams;

	public AuthorizationResult(boolean isAuthorized) {
		this.isAuthorized = isAuthorized;
		this.storeParams = Collections.emptyMap();
	}

	public AuthorizationResult(boolean isAuthorized, Map<String, Object> storeParams) {
		this.isAuthorized = isAuthorized;
		this.storeParams = isAuthorized && storeParams != null ?
				Collections.unmodifiableMap(storeParams) : Collections.emptyMap();
	}

	/**
	 * @return <b>true</b> if a client is authorized, otherwise - <b>false</b>
	 * */
	public boolean isAuthorized() {
		return isAuthorized;
	}

	/**
	 * @return key-value pairs (unmodifiable) that will be added to {@link SocketIOClient } store.
	 * If a client is not authorized, storeParams will always be ignored (empty map)
	 * */
	public Map<String, Object> getStoreParams() {
		return storeParams;
	}
}
