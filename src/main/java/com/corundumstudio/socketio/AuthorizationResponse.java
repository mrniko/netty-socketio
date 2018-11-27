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

import java.util.Collections;
import java.util.Map;

public class AuthorizationResponse {
    public enum Action {
        CONNECT,
        REDIRECT,
        DISCONNECT
    }

    private final Action action;
    private final Map<String, Object> data;

    private AuthorizationResponse(Action action, Map<String, Object> data) {
        if (data == null) {
            data = Collections.emptyMap();
        }

        this.action = action;
        this.data = data;
    }

    public static AuthorizationResponse connect() {
        return new AuthorizationResponse(Action.CONNECT, null);
    }

    public static AuthorizationResponse connect(String key, Object value) {
        return new AuthorizationResponse(Action.CONNECT, Collections.singletonMap(key, value));
    }

    public static AuthorizationResponse connect(Map<String, Object> data) {
        return new AuthorizationResponse(Action.CONNECT, data);
    }

    public static AuthorizationResponse redirect(String locationUrl) {
        return new AuthorizationResponse(Action.REDIRECT, Collections.singletonMap("Location", (Object) locationUrl));
    }

    public static AuthorizationResponse disconnect() {
        return new AuthorizationResponse(Action.DISCONNECT, null);
    }

    public Action getAction() {
        return action;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
