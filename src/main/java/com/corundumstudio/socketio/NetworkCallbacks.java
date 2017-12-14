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

import com.corundumstudio.socketio.concurrent.FailedFuture;
import com.corundumstudio.socketio.concurrent.SucceededFuture;
import io.netty.util.concurrent.ImmediateEventExecutor;

/**
 * Object instances of {@link NetworkCallback}
 */
public class NetworkCallbacks {
    private static Throwable CHANNEL_CLOSED_ERROR = new IllegalStateException("channel is closed");

    public static <T> NetworkCallback<T> channelClosed()  {
        return new FailedFuture<T>(ImmediateEventExecutor.INSTANCE, CHANNEL_CLOSED_ERROR);
    }

    public static <T> NetworkCallback<T> success(T value)  {
        return new SucceededFuture<T>(ImmediateEventExecutor.INSTANCE, value);
    }

    public static <E extends Throwable> NetworkCallback<E> failure(E throwable)  {
        return new FailedFuture<E>(ImmediateEventExecutor.INSTANCE, throwable);
    }
}
