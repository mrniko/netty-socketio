package com.corundumstudio.socketio.namespace;

import com.corundumstudio.socketio.Configuration;
import com.corundumstudio.socketio.HttpRequestBody;
import com.corundumstudio.socketio.HttpRequestSignature;
import com.corundumstudio.socketio.HttpResponse;
import com.corundumstudio.socketio.listener.ExceptionListener;
import com.corundumstudio.socketio.listener.HttpListener;
import com.corundumstudio.socketio.listener.HttpListeners;
import io.netty.handler.codec.http.HttpHeaders;
import io.netty.handler.codec.http.HttpMethod;
import io.netty.util.internal.PlatformDependent;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

public class HttpNamespace implements HttpListeners {

    private final ConcurrentMap<HttpRequestSignature, HttpListener> httpListeners = PlatformDependent.newConcurrentHashMap();
    private final ExceptionListener exceptionListener;

    public HttpNamespace(Configuration configuration) {
        this.exceptionListener = configuration.getExceptionListener();
    }

    public void addHttpListener(HttpMethod method, String path, HttpListener listener) {
        HttpRequestSignature signature = new HttpRequestSignature(method, path);
        httpListeners.put(signature, listener);
    }

    public boolean hasListeners() {
        return !httpListeners.isEmpty();
    }

    public HttpResponse onRequest(HttpRequestSignature httpRequestSignature, Map<String, List<String>> params, HttpHeaders headers, HttpRequestBody body) {
        HttpListener httpListener = httpListeners.get(httpRequestSignature);

        try {
            return httpListener.onRequest(params, headers, body);
        } catch (Exception e) {
            exceptionListener.onHttpException(e, httpRequestSignature);
            return null;
        }
    }

}
