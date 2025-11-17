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

import java.io.Serializable;
import java.net.InetSocketAddress;
import java.util.Date;
import java.util.List;
import java.util.Map;

import io.netty.handler.codec.http.HttpHeaders;

public class HandshakeData implements Serializable {

    private static final long serialVersionUID = 1196350300161819978L;

    private HttpHeaders headers;
    private InetSocketAddress address;
    private Date time = new Date();
    private InetSocketAddress local;
    private String url;
    private Map<String, List<String>> urlParams;
    private boolean xdomain;
    private Object authToken;

    // needed for correct deserialization
    public HandshakeData() {
    }

    public HandshakeData(HttpHeaders headers, Map<String, List<String>> urlParams, InetSocketAddress address, String url, boolean xdomain) {
        this(headers, urlParams, address, null, url, xdomain);
    }

    public HandshakeData(HttpHeaders headers, Map<String, List<String>> urlParams, InetSocketAddress address, InetSocketAddress local, String url, boolean xdomain) {
        super();
        this.headers = headers;
        this.urlParams = urlParams;
        this.address = address;
        this.local = local;
        this.url = url;
        this.xdomain = xdomain;
    }

    /**
     * Client network address
     *
     * @return network address
     */
    public InetSocketAddress getAddress() {
        return address;
    }

    /**
     * Connection local address
     *
     * @return local address
     */
    public InetSocketAddress getLocal() {
        return local;
    }

    /**
     * Http headers sent during first client request
     *
     * @return headers
     */
    public HttpHeaders getHttpHeaders() {
        return headers;
    }

    /**
     * Client connection date
     *
     * @return date
     */
    public Date getTime() {
        return time;
    }

    /**
     * Url used by client during first request
     *
     * @return url
     */
    public String getUrl() {
        return url;
    }

    public boolean isXdomain() {
        return xdomain;
    }

    /**
     * Url params stored in url used by client during first request
     *
     * @return map
     */
    public Map<String, List<String>> getUrlParams() {
        return urlParams;
    }

    public String getSingleUrlParam(String name) {
        List<String> values = urlParams.get(name);
        if (values != null && values.size() == 1) {
            return values.iterator().next();
        }
        return null;
    }

    public void setAuthToken(Object token) {
        this.authToken = token;
    }

    public Object getAuthToken() {
        return this.authToken;
    }
}
