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
import io.netty.util.CharsetUtil;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;

/*
 * Used to return a result from <b>AuthorizationListener</b>
 * Used to add data to the client store
 */
public class AuthorizationResponse {

    private final HttpResponseStatus httpResponseStatus;
    private final HttpHeaders httpHeaders = new DefaultHttpHeaders();
    private String body;
    private Charset charset = CharsetUtil.UTF_8;
    // data for the client store
    private final Map<String, Object> clientData = new HashMap<String, Object>();

    public AuthorizationResponse(HttpResponseStatus httpResponseStatus) {
        this.httpResponseStatus = httpResponseStatus;
    }

    public static AuthorizationResponse OK() {
        return new AuthorizationResponse(HttpResponseStatus.OK);
    }

    public static AuthorizationResponse TEMPORARY_REDIRECT(String locationUrl) {
        AuthorizationResponse authorizationResponse = new AuthorizationResponse(HttpResponseStatus.TEMPORARY_REDIRECT);
        authorizationResponse.getHeaders().add("Location", locationUrl);
        return authorizationResponse;
    }

    public static AuthorizationResponse UNAUTHORIZED() {
        return new AuthorizationResponse(HttpResponseStatus.UNAUTHORIZED);
    }

    public static AuthorizationResponse INTERNAL_SERVER_ERROR() {
        return new AuthorizationResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }

    public AuthorizationResponse setHeader(String name, String value) {
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

    public AuthorizationResponse setBody(String body) {
        this.body = body;
        return this;
    }

    public AuthorizationResponse setBody(String body, Charset charset) {
        this.body = body;
        this.charset = charset;
        return this;
    }

    public HttpResponseStatus getHttpResponseStatus() {
        return httpResponseStatus;
    }

    public String getBody() {
        return body;
    }

    public Charset getCharset() {
        return charset;
    }

    /*
     * Adds
     */
    public AuthorizationResponse setClientData(String key, Object value) {
        clientData.put(key, value);
        return this;
    }

    public Map<String, Object> getClientData() {
        return clientData;
    }

}
