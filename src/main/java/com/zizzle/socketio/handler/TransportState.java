/**
 * Copyright (c) 2012-2023 Nikita Koksharov
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.zizzle.socketio.handler;

import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

import com.zizzle.socketio.protocol.Packet;

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
