/**
 * Copyright (c) 2012 Nikita Koksharov
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the "Software"), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
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
	
	@Override
	public Channel getChannel() {
		throw new UnsupportedOperationException();
	}

	@Override
	public boolean isDone() {
		return true;
	}

	@Override
	public boolean isCancelled() {
		return false;
	}

	@Override
	public boolean isSuccess() {
		return false;
	}

	@Override
	public Throwable getCause() {
		return null;
	}

	@Override
	public boolean cancel() {
		return false;
	}

	@Override
	public boolean setSuccess() {
		return false;
	}

	@Override
	public boolean setFailure(Throwable cause) {
		return false;
	}

	@Override
	public boolean setProgress(long amount, long current, long total) {
		return false;
	}

	@Override
	public void addListener(ChannelFutureListener listener) {
		try {
			listener.operationComplete(this);
		} catch (Exception e) {
			log.error("Can't execute ChannelFutureListener ", e);
		}
	}

	@Override
	public void removeListener(ChannelFutureListener listener) {
	}

	@Override
	public ChannelFuture await() throws InterruptedException {
		return this;
	}

	@Override
	public ChannelFuture awaitUninterruptibly() {
		return this;
	}

	@Override
	public boolean await(long timeout, TimeUnit unit)
			throws InterruptedException {
		return true;
	}

	@Override
	public boolean await(long timeoutMillis) throws InterruptedException {
		return true;
	}

	@Override
	public boolean awaitUninterruptibly(long timeout, TimeUnit unit) {
		return true;
	}

	@Override
	public boolean awaitUninterruptibly(long timeoutMillis) {
		return true;
	}

}
