package com.corundumstudio.socketio.listener;

import com.corundumstudio.socketio.HttpRequestBody;
import com.corundumstudio.socketio.HttpResponse;
import io.netty.handler.codec.http.HttpHeaders;

import java.util.List;
import java.util.Map;

public interface HttpListener {

    HttpResponse onRequest(Map<String, List<String>> params, HttpHeaders headers, HttpRequestBody body) throws Exception;

}
