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

public abstract class AckCallback {

    private int timeout = -1;

    public AckCallback() {
    }

    /**
     * Creates AckCallback
     *
     * @param timeout - callback timeout in seconds
     */
    public AckCallback(int timeout) {
        this.timeout = timeout;
    }

    public int getTimeout() {
        return timeout;
    }

    public abstract void onSuccess();

    /**
     * Invoked only once then <code>timeout</code> defined
     *
     */
    public void onTimeout() {

    }

}
