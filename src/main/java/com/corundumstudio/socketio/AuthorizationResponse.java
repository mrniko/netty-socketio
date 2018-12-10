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

import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.Collections;
import java.util.Map;

/*
 * Used to return a result from <b>AuthorizationListener</b>
 *
 * connect - authorizes and connects the socket, puts data in the client store
 * redirect - returns <b>307</b> with the <b>Location</b> header set to the new location, then disconnects
 * disconnect - returns the indicated HttpResponseStatus with headers, or <b>401 Unauthorized</b> if not set
 */
public class AuthorizationResponse {

    private final HttpResponseStatus httpResponseStatus;
    private final Map<String, Object> data;

    private AuthorizationResponse(HttpResponseStatus httpResponseStatus, Map<String, Object> data) {
        this.httpResponseStatus = httpResponseStatus;
        this.data = data;
    }

    public static AuthorizationResponse connect() {
        return new AuthorizationResponse(HttpResponseStatus.OK, null);
    }

    public static AuthorizationResponse connect(String key, Object value) {
        return new AuthorizationResponse(HttpResponseStatus.OK, Collections.singletonMap(key, value));
    }

    public static AuthorizationResponse connect(Map<String, Object> data) {
        return new AuthorizationResponse(HttpResponseStatus.OK, data);
    }

    public static AuthorizationResponse redirect(String locationUrl) {
        return new AuthorizationResponse(HttpResponseStatus.PERMANENT_REDIRECT, Collections.singletonMap("Location", (Object) locationUrl));
    }

    public static AuthorizationResponse disconnect() {
        return new AuthorizationResponse(HttpResponseStatus.UNAUTHORIZED, null);
    }

    public static AuthorizationResponse disconnect(HttpResponseStatus httpResponseStatus) {
        return new AuthorizationResponse(httpResponseStatus, null);
    }

    public static AuthorizationResponse disconnect(HttpResponseStatus httpResponseStatus, String header, String value) {
        return new AuthorizationResponse(httpResponseStatus, Collections.singletonMap(header, (Object) value));
    }

    public static AuthorizationResponse disconnect(HttpResponseStatus httpResponseStatus, Map<String, String> headers) {
        return new AuthorizationResponse(httpResponseStatus, Collections.<String, Object>unmodifiableMap(headers));
    }

    public HttpResponseStatus getHttpResponseStatus() {
        return httpResponseStatus;
    }

    public Map<String, Object> getData() {
        return data;
    }
}
