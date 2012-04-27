/**
 * Copyright 2012 Nikita Koksharov
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
package com.corundumstudio.socketio;

import java.util.concurrent.TimeUnit;

import org.jboss.netty.channel.Channel;
import org.jboss.netty.channel.ChannelFuture;
import org.jboss.netty.channel.ChannelFutureListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public class NullChannelFuture implements ChannelFuture {

    public static final ChannelFuture INSTANCE = new NullChannelFuture();

    private final Logger log = LoggerFactory.getLogger(getClass());

    public Channel getChannel() {
        throw new UnsupportedOperationException();
    }

    public boolean isDone() {
        return true;
    }

    public boolean isCancelled() {
        return false;
    }

    public boolean isSuccess() {
        return false;
    }

    public Throwable getCause() {
        return null;
    }

    public boolean cancel() {
        return false;
    }

    public boolean setSuccess() {
        return false;
    }

    public boolean setFailure(Throwable cause) {
        return false;
    }

    public boolean setProgress(long amount, long current, long total) {
        return false;
    }

    public void addListener(ChannelFutureListener listener) {
        try {
            listener.operationComplete(this);
        } catch (Exception e) {
            log.error("Can't execute ChannelFutureListener ", e);
        }
    }

    public void removeListener(ChannelFutureListener listener) {
    }

    public ChannelFuture await() throws InterruptedException {
        return this;
    }

    public ChannelFuture awaitUninterruptibly() {
        return this;
    }

    public boolean await(long timeout, TimeUnit unit) throws InterruptedException {
        return true;
    }

    public boolean await(long timeoutMillis) throws InterruptedException {
        return true;
    }

    public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
        return true;
    }

    public boolean awaitUninterruptibly(long timeoutMillis) {
        return true;
    }

    public ChannelFuture rethrowIfFailed() throws Exception {
        return this;
    }

}
