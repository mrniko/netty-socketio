package com.corundumstudio.socketio.messages;

import com.corundumstudio.socketio.Transport;
import com.corundumstudio.socketio.handler.ClientHead;

public class OutPacketMessage extends HttpMessage {

    ClientHead clientHead;
    Transport transport;

    public OutPacketMessage(ClientHead clientHead, Transport transport) {
        super(clientHead.getOrigin(), clientHead.getSessionId());

        this.clientHead = clientHead;
        this.transport = transport;
    }

    public Transport getTransport() {
        return transport;
    }

    public ClientHead getClientHead() {
        return clientHead;
    }

}
