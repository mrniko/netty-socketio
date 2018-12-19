package com.corundumstudio.socketio;

import java.util.List;
import java.util.Map;

public class HttpParams {
    private final Map<String, List<String>> params;

    public HttpParams(Map<String, List<String>> params) {
        this.params = params;
    }

    public String get(String header) {
        List<String> values = getAll(header);
        if (values.isEmpty()) return null;

        return values.get(0);
    }

    public List<String> getAll(String header) {
        return params.get(header);
    }
}
