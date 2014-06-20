package com.corundumstudio.socketio.handler;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.corundumstudio.socketio.protocol.Packet;

import io.netty.channel.Channel;

public class TransportState {

    private Queue<Packet> packetsQueue = new ConcurrentLinkedQueue<Packet>();
    private Channel channel;

    public void setPacketsQueue(Queue<Packet> packetsQueue) {
        this.packetsQueue = packetsQueue;
    }

    public Queue<Packet> getPacketsQueue() {
        return packetsQueue;
    }

    public Channel getChannel() {
        return channel;
    }

    public Channel update(Channel channel) {
        Channel prevChannel = this.channel;
        this.channel = channel;
        return prevChannel;
    }

}
