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
import io.netty.util.CharsetUtil;

import java.nio.charset.Charset;

/*
 * Used to return a result from <b>HttpListener</b>
 */
public class HttpResponse {

    private final HttpResponseStatus httpResponseStatus;
    private final HttpHeaders httpHeaders = new DefaultHttpHeaders();
    private String body;
    private String contentType = "text/plain";
    private Charset charset = CharsetUtil.UTF_8;

    public HttpResponse(HttpResponseStatus httpResponseStatus) {
        this.httpResponseStatus = httpResponseStatus;
    }

    public static HttpResponse OK() {
        return new HttpResponse(HttpResponseStatus.OK);
    }

    public static HttpResponse TEMPORARY_REDIRECT(String locationUrl) {
        HttpResponse authorizationResponse = new HttpResponse(HttpResponseStatus.TEMPORARY_REDIRECT);
        authorizationResponse.getHeaders().add(HttpHeaderNames.LOCATION, locationUrl);
        return authorizationResponse;
    }

    public static HttpResponse UNAUTHORIZED() {
        return new HttpResponse(HttpResponseStatus.UNAUTHORIZED);
    }

    public static HttpResponse INTERNAL_SERVER_ERROR() {
        return new HttpResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }

    public HttpResponse setHeader(String name, String value) {
        httpHeaders.add(name, value);
        return this;
    }

    public HttpResponse setHeaders(HttpHeaders headers) {
        httpHeaders.setAll(headers);
        return this;
    }

    public HttpHeaders getHeaders() {
        return httpHeaders;
    }

    public HttpResponse setBody(String body) {
        this.body = body;
        return this;
    }

    public HttpResponse setBody(String body, String contentType) {
        this.body = body;
        this.contentType = contentType;
        return this;
    }

    public HttpResponse setBody(String body, String contentType, Charset charset) {
        this.body = body;
        this.contentType = contentType;
        this.charset = charset;
        return this;
    }

    public HttpResponseStatus getHttpResponseStatus() {
        return httpResponseStatus;
    }

    public String getBody() {
        return body;
    }

    public String getContentType() {
        return contentType;
    }

    public Charset getCharset() {
        return charset;
    }
}
