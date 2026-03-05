package ru.practicum.moviehub.http.util;

import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;

public class QueryUtils {

    public static Map<String, List<String>> queryParams(String query) {

        if (query == null || query.isEmpty()) {
            return Collections.emptyMap();
        }

        Map<String, List<String>> result = new HashMap<>();

        String[] pairs = query.split("&");

        for (String pair : pairs) {

            String[] keyValue = pair.split("=", 2);

            String key = safeDecode(keyValue[0]);
            String value = safeDecode(keyValue.length > 1 ? keyValue[1] : "");

            result.computeIfAbsent(key, k -> new ArrayList<>()).add(value);
        }

        return result;
    }

    private static String safeDecode(String str) {
        try {
            return URLDecoder.decode(str, StandardCharsets.UTF_8);
        } catch (Exception e) {
            return str;
        }
    }
}
