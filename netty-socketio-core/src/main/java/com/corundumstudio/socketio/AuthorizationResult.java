/**
 * Copyright (c) 2012-2025 Nikita Koksharov
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

public class AuthorizationResult {

    public static final AuthorizationResult SUCCESSFUL_AUTHORIZATION = new AuthorizationResult(true);
    public static final AuthorizationResult FAILED_AUTHORIZATION = new AuthorizationResult(false);
    private final boolean isAuthorized;
    private final Map<String, Object> storeParams;

    public AuthorizationResult(boolean isAuthorized) {
        this.isAuthorized = isAuthorized;
        this.storeParams = Collections.emptyMap();
    }

    public AuthorizationResult(boolean isAuthorized, Map<String, Object> storeParams) {
        this.isAuthorized = isAuthorized;
        if (isAuthorized && storeParams != null) {
            this.storeParams = Collections.unmodifiableMap(storeParams);
        } else {
            this.storeParams = Collections.emptyMap();
        }
    }

    /**
     * @return <b>true</b> if a client is authorized, otherwise - <b>false</b>
     * */
    public boolean isAuthorized() {
        return isAuthorized;
    }

    /**
     * @return key-value pairs (unmodifiable) that will be added to {@link SocketIOClient } store.
     * If a client is not authorized, storeParams will always be ignored (empty map)
     * */
    public Map<String, Object> getStoreParams() {
        return storeParams;
    }
}
