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

import io.netty.handler.codec.http.DefaultHttpHeaders;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;

import java.util.HashMap;
import java.util.Map;

/*
 * Used to return a result from <b>AuthorizationListener</b>
 *
 * connect - authorizes and connects the socket, puts storeData in the client store
 * redirect - returns <b>307</b> with the <b>Location</b> header set to the new location, then disconnects
 * disconnect - returns the indicated HttpResponseStatus with headers, or <b>401 Unauthorized</b> if not set
 */
public class AuthorizationResponse {

    private final HttpResponseStatus httpResponseStatus;
    private final HttpHeaders httpHeaders = new DefaultHttpHeaders();
    private final Map<String, Object> storeData = new HashMap<String, Object>();

    private AuthorizationResponse(HttpResponseStatus httpResponseStatus) {
        this.httpResponseStatus = httpResponseStatus;
    }

    public static AuthorizationResponse connect() {
        return new AuthorizationResponse(HttpResponseStatus.OK);
    }

    public static AuthorizationResponse redirect(String locationUrl) {
        AuthorizationResponse authorizationResponse = new AuthorizationResponse(HttpResponseStatus.PERMANENT_REDIRECT);
        authorizationResponse.getHttpHeaders().add("Location", locationUrl);
        return authorizationResponse;
    }

    public static AuthorizationResponse disconnect() {
        return new AuthorizationResponse(HttpResponseStatus.UNAUTHORIZED);
    }

    public static AuthorizationResponse disconnect(HttpResponseStatus httpResponseStatus) {
        return new AuthorizationResponse(httpResponseStatus);
    }

    public AuthorizationResponse addHttpHeader(String name, String value) {
        httpHeaders.add(name, value);
        return this;
    }

    public AuthorizationResponse addStoreData(String key, String value) {
        if (!HttpResponseStatus.OK.equals(httpResponseStatus)) {
            throw new UnsupportedOperationException("`storeData` only allowed for 'connect'.");
        }

        storeData.put(key, value);
        return this;
    }

    public HttpResponseStatus getHttpResponseStatus() {
        return httpResponseStatus;
    }

    public HttpHeaders getHttpHeaders() {
        return httpHeaders;
    }

    public Map<String, Object> getStoreData() {
        return storeData;
    }
}
