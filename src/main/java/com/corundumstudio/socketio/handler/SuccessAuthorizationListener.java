package com.corundumstudio.socketio.handler;

import com.corundumstudio.socketio.AuthorizationListener;
import com.corundumstudio.socketio.HandshakeData;

public class SuccessAuthorizationListener implements AuthorizationListener {

    @Override
    public boolean isAuthorized(HandshakeData data) {
        return true;
    }

}
