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

import java.util.HashMap;
import java.util.Map;

/*
 * Used to return a result from <b>AuthorizationListener</b>
 *
 * OK - authorizes and connects the socket, puts storeData in the client store
 * TEMPORARY_REDIRECT - returns <b>307</b> with the <b>Location</b> header set to the new location, then disconnects
 * UNAUTHORIZED - returns the indicated HttpResponseStatus with headers, or <b>401 Unauthorized</b> if not set
 */
public class AuthorizationResponse extends HttpResponse {

    private final Map<String, Object> storeData = new HashMap<String, Object>();

    public AuthorizationResponse(HttpResponseStatus httpResponseStatus) {
        super(httpResponseStatus);
    }

    public static AuthorizationResponse OK() {
        return new AuthorizationResponse(HttpResponseStatus.OK);
    }

    public static AuthorizationResponse TEMPORARY_REDIRECT(String locationUrl) {
        AuthorizationResponse httpResponse = new AuthorizationResponse(HttpResponseStatus.TEMPORARY_REDIRECT);
        httpResponse.getHeaders().add("Location", locationUrl);
        return httpResponse;
    }

    public static AuthorizationResponse UNAUTHORIZED() {
        return new AuthorizationResponse(HttpResponseStatus.UNAUTHORIZED);
    }

    public AuthorizationResponse addStoreData(String key, Object value) {
        storeData.put(key, value);
        return this;
    }

    public Map<String, Object> getStoreData() {
        return storeData;
    }
}
