package com.corundumstudio.socketio;

import io.netty.handler.codec.http.HttpMethod;

public class HttpRequestSignature {
    private final HttpMethod httpMethod;
    private final String path;

    public HttpRequestSignature(HttpMethod httpMethod, String path) {
        this.httpMethod = httpMethod;
        this.path = path;
    }

    public HttpMethod getHttpMethod() {
        return httpMethod;
    }

    public String getPath() {
        return path;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof HttpRequestSignature)) return false;

        HttpRequestSignature that = (HttpRequestSignature) o;

        if (!httpMethod.equals(that.httpMethod)) return false;
        return path.equals(that.path);
    }

    @Override
    public int hashCode() {
        int result = httpMethod.hashCode();
        result = 31 * result + path.hashCode();
        return result;
    }
}
