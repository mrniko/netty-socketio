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
import io.netty.util.CharsetUtil;

import java.nio.charset.Charset;
import java.util.Map;

/*
 * Used to reject client
 */
public class UnauthorizedResponse extends AuthorizationResponse {

    private final HttpHeaders httpHeaders = new DefaultHttpHeaders();
    private String body;
    private String contentType = "text/plain";
    private Charset charset = CharsetUtil.UTF_8;

    public UnauthorizedResponse(HttpResponseStatus httpResponseStatus) {
        super(validateHttpResponseStatus(httpResponseStatus));
    }

    private static HttpResponseStatus validateHttpResponseStatus(HttpResponseStatus httpResponseStatus) {
        if (HttpResponseStatus.OK.equals(httpResponseStatus)) {
            throw new RuntimeException("Use 'AuthorizedResponse' for httpResponseStatus 'OK'.");
        }
        return httpResponseStatus;
    }

    public static UnauthorizedResponse TEMPORARY_REDIRECT(String locationUrl) {
        UnauthorizedResponse authorizationResponse = new UnauthorizedResponse(HttpResponseStatus.TEMPORARY_REDIRECT);
        authorizationResponse.getHeaders().add(HttpHeaderNames.LOCATION, locationUrl);
        return authorizationResponse;
    }

    public static UnauthorizedResponse UNAUTHORIZED() {
        return new UnauthorizedResponse(HttpResponseStatus.UNAUTHORIZED);
    }

    public static UnauthorizedResponse INTERNAL_SERVER_ERROR() {
        return new UnauthorizedResponse(HttpResponseStatus.INTERNAL_SERVER_ERROR);
    }

    public UnauthorizedResponse setHeader(AsciiString name, String value) {
        httpHeaders.add(name, value);
        return this;
    }

    public UnauthorizedResponse setHeaders(HttpHeaders headers) {
        httpHeaders.setAll(headers);
        return this;
    }

    public HttpHeaders getHeaders() {
        return httpHeaders;
    }

    public UnauthorizedResponse setBody(String body) {
        this.body = body;
        return this;
    }

    public UnauthorizedResponse setBody(String body, String contentType) {
        this.body = body;
        this.contentType = contentType;
        return this;
    }

    public UnauthorizedResponse setBody(String body, String contentType, Charset charset) {
        this.body = body;
        this.contentType = contentType;
        this.charset = charset;
        return this;
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
