package ru.practicum.moviehub.http.request;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.http.util.QueryUtils;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class RequestParser {

    public static RequestContext parse(HttpExchange ex) {

        String method = ex.getRequestMethod();
        URI uri = ex.getRequestURI();
        String query = uri.getQuery();
        List<String> segments = parsePath(uri);
        Map<String, List<String>> queryParams = QueryUtils.queryParams(query);

        return new RequestContext(method, segments, queryParams);
    }

    private static List<String> parsePath(URI uri) {
        return Arrays.stream(uri.getPath().split("/"))
                .filter(s -> !s.isEmpty()).collect(Collectors.toList());
    }
}
