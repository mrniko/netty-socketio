package com.corundumstudio.socketio;

import java.util.List;
import java.util.Map;
import java.util.Set;

public class HttpParams {
    private final Map<String, List<String>> params;

    public HttpParams(Map<String, List<String>> params) {
        this.params = params;
    }

    public Set<String> getNames() {
        return params.keySet();
    }

    public String get(String name) {
        List<String> values = getAll(name);
        if (values == null || values.isEmpty()) return null;

        return values.get(0);
    }

    public List<String> getAll(String name) {
        return params.get(name);
    }

    public Map<String, List<String>> asMap() {
        return params;
    }

    @Override
    public String toString() {
        return params.toString();
    }
}
