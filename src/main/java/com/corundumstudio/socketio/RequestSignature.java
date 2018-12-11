package com.corundumstudio.socketio;

import io.netty.handler.codec.http.HttpMethod;

public class RequestSignature {
    private final HttpMethod httpMethod;
    private final String path;

    public RequestSignature(HttpMethod httpMethod, String path) {
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
        if (!(o instanceof RequestSignature)) return false;

        RequestSignature that = (RequestSignature) o;

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
