package com.corundumstudio.socketio.listener;

import com.corundumstudio.socketio.HttpParams;
import com.corundumstudio.socketio.HttpRequestBody;
import com.corundumstudio.socketio.HttpResponse;
import io.netty.handler.codec.http.HttpHeaders;

public interface HttpListener {

    HttpResponse onRequest(HttpParams params, HttpHeaders headers, HttpRequestBody body) throws Exception;

}
