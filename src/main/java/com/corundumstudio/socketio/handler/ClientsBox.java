package com.corundumstudio.socketio.handler;

import io.netty.channel.Channel;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import com.corundumstudio.socketio.HandshakeData;

public class ClientsBox {

    private final Map<UUID, ClientHead> uuid2clients = new ConcurrentHashMap<UUID, ClientHead>();
    private final Map<Channel, ClientHead> channel2clients = new ConcurrentHashMap<Channel, ClientHead>();

    // TODO use storeFactory
    public HandshakeData getHandshakeData(UUID sessionId) {
        ClientHead client = uuid2clients.get(sessionId);
        if (client == null) {
            return null;
        }

        return client.getHandshakeData();
    }

    public void addClient(ClientHead clientHead) {
        uuid2clients.put(clientHead.getSessionId(), clientHead);
    }

    public void removeClient(UUID sessionId) {
        uuid2clients.remove(sessionId);
    }

    public ClientHead get(UUID sessionId) {
        return uuid2clients.get(sessionId);
    }

    public void add(Channel channel, ClientHead clientHead) {
        channel2clients.put(channel, clientHead);
    }

    public void remove(Channel channel) {
        channel2clients.remove(channel);
    }


    public ClientHead get(Channel channel) {
        return channel2clients.get(channel);
    }

}
