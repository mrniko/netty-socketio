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

import io.netty.handler.codec.http.HttpHeaderNames;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpResponseStatus;
import io.netty.util.AsciiString;

import java.util.HashMap;
import java.util.Map;

/*
 * Used to authorize client and add data to the client store
 */
public class AuthorizedResponse extends AuthorizationResponse {

    private final Map<String, Object> clientData = new HashMap<String, Object>();

    public AuthorizedResponse() {
        super(HttpResponseStatus.OK);
    }

    public static AuthorizedResponse OK() {
        return new AuthorizedResponse();
    }

    public AuthorizedResponse setClientData(String key, Object value) {
        clientData.put(key, value);
        return this;
    }

    public AuthorizedResponse setClientData(Map<String, Object> map) {
        clientData.putAll(map);
        return this;
    }


    public Map<String, Object> getClientData() {
        return clientData;
    }

}
