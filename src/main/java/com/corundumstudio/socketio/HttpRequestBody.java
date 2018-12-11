package com.corundumstudio.socketio;

import io.netty.buffer.ByteBuf;
import io.netty.handler.codec.http.FullHttpRequest;

import java.nio.charset.Charset;

public class HttpRequestBody {

    private final FullHttpRequest req;

    public HttpRequestBody(FullHttpRequest req) {
        this.req = req;
    }

    public String toString() {
        return toString(Charset.defaultCharset());
    }

    public String toString(Charset charset) {
        ByteBuf buffer = req.content();
        return buffer.toString(charset);
    }
}
