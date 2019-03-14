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
package com.corundumstudio.socketio.concurrent;

import io.netty.util.concurrent.EventExecutor;
import io.netty.util.concurrent.Future;
import io.netty.util.internal.PlatformDependent;

public final class FailedFuture<V> extends CompleteFuture<V> {
    private final Throwable cause;

    public FailedFuture(EventExecutor executor, Throwable cause) {
        super(executor);
        if (cause == null) {
            throw new NullPointerException("cause");
        } else {
            this.cause = cause;
        }
    }

    public Throwable cause() {
        return this.cause;
    }

    public boolean isSuccess() {
        return false;
    }

    public Future<V> sync() {
        PlatformDependent.throwException(this.cause);
        return this;
    }

    public Future<V> syncUninterruptibly() {
        PlatformDependent.throwException(this.cause);
        return this;
    }

    public V getNow() {
        return null;
    }
}
