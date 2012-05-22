package com.corundumstudio.socketio;

import java.util.Collections;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;
import java.util.concurrent.atomic.AtomicLong;

import com.corundumstudio.socketio.parser.Packet;

public class AckManager implements Disconnectable {

    private final AtomicLong ackIndex = new AtomicLong();
    private final Map<Long, Runnable> ackCallbacks = new ConcurrentHashMap<Long, Runnable>();
    private final ConcurrentMap<UUID, Set<Long>> clientCallbackIds = new ConcurrentHashMap<UUID, Set<Long>>();

    public void onAck(SocketIOClient client, Packet packet) {
        Runnable callback = ackCallbacks.remove(packet.getAckId());
        if (callback != null) {
            Set<Long> callbackIds = clientCallbackIds.get(client.getSessionId());
            if (callbackIds != null) {
                callbackIds.remove(packet.getAckId());
            }
            callback.run();
        }
    }

    public long registerAck(UUID sessionId, Runnable callback) {
        Set<Long> callbackIds = clientCallbackIds.get(sessionId);
        if (callbackIds == null) {
            callbackIds = Collections.newSetFromMap(new ConcurrentHashMap<Long, Boolean>());
            Set<Long> oldCallbackIds = clientCallbackIds.putIfAbsent(sessionId, callbackIds);
            if (oldCallbackIds != null) {
                callbackIds = oldCallbackIds;
            }
        }
        long index = ackIndex.incrementAndGet();
        callbackIds.add(index);
        ackCallbacks.put(index, callback);
        return index;
    }

    @Override
    public void onDisconnect(SocketIOClient client) {
        Set<Long> callbackIds = clientCallbackIds.remove(client.getSessionId());
        if (callbackIds != null) {
            ackCallbacks.keySet().removeAll(callbackIds);
        }
    }

}
