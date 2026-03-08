package ru.practicum.moviehub.http.request;

import java.util.List;
import java.util.Map;

public class RequestContext {
    private final String method;
    private final List<String> pathSegments;
    private final Map<String, List<String>> queryParams;

    public RequestContext(String method, List<String> pathSegments, Map<String, List<String>> queryParams) {
        this.method = method;
        this.pathSegments = pathSegments;
        this.queryParams = queryParams;
    }

    public String getMethod() {
        return method;
    }

    public List<String> getPathSegments() {
        return pathSegments;
    }

    public Map<String, List<String>> getQueryParams() {
        return queryParams;
    }
}
