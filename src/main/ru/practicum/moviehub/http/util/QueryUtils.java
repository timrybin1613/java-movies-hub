package ru.practicum.moviehub.http.util;

import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;

public class QueryUtils {

    public static Map<String, List<String>> queryParams(String query) {

        if (query == null || query.isEmpty()) {
            return Collections.emptyMap();
        }

        return Arrays.stream(query.split("&"))
                .map(pair -> pair.split("=", 2))
                .collect(Collectors.groupingBy(
                        kv -> safeDecode(kv[0]),
                        Collectors.mapping(
                                kv -> safeDecode(kv.length > 1 ? kv[1] : ""),
                                Collectors.toList())));
    }

    private static String safeDecode(String str) {
        try {
            return URLDecoder.decode(str, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return str;
        }
    }
}