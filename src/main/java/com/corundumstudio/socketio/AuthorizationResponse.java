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
import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.AsciiString;

import java.util.HashMap;
import java.util.Map;

/*
 * Used to add data to the client store
 */
public class AuthorizationResponse {

    private final HttpResponseStatus httpResponseStatus;
    private final HttpHeaders httpHeaders = new DefaultHttpHeaders();
    private final Map<String, Object> clientData = new HashMap<String, Object>();

    public AuthorizationResponse(HttpResponseStatus httpResponseStatus) {
        this.httpResponseStatus = httpResponseStatus;
    }

    public static AuthorizationResponse OK() {
        return new AuthorizationResponse(HttpResponseStatus.OK);
    }

    public static AuthorizationResponse TEMPORARY_REDIRECT(String locationUrl) {
        AuthorizationResponse authorizationResponse = new AuthorizationResponse(HttpResponseStatus.TEMPORARY_REDIRECT);
        authorizationResponse.getHeaders().add(HttpHeaderNames.LOCATION, locationUrl);
        return authorizationResponse;
    }

    public static AuthorizationResponse UNAUTHORIZED() {
        return new AuthorizationResponse(HttpResponseStatus.UNAUTHORIZED);
    }

    public static AuthorizationResponse INTERNAL_SERVER_ERROR() {
        return new AuthorizationResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }

    public AuthorizationResponse setHeader(AsciiString name, String value) {
        httpHeaders.add(name, value);
        return this;
    }

    public AuthorizationResponse setHeaders(HttpHeaders headers) {
        httpHeaders.setAll(headers);
        return this;
    }

    public HttpHeaders getHeaders() {
        return httpHeaders;
    }

    public HttpResponseStatus getHttpResponseStatus() {
        return httpResponseStatus;
    }

    public AuthorizationResponse setClientData(String key, Object value) {
        clientData.put(key, value);
        return this;
    }

    public AuthorizationResponse setClientData(Map<String, Object> map) {
        clientData.putAll(map);
        return this;
    }


    public Map<String, Object> getClientData() {
        return clientData;
    }

}
