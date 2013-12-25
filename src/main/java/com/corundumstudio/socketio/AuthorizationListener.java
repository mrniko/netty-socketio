package com.corundumstudio.socketio;

public interface AuthorizationListener {

    /**
     * Checks is client with handshake data is authorized
     *
     * @param data - handshake data
     * @return - <b>true</b> if client is authorized of <b>false</b> otherwise
     */
    boolean isAuthorized(HandshakeData data);

}
