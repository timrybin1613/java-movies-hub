package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CT_JSON = "application/json; charset=UTF-8";

    protected void sendJson(HttpExchange ex, int status, String json) throws IOException {
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);
        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void send(HttpExchange ex, int code) throws java.io.IOException {
        ex.sendResponseHeaders(code, 0);
        ex.close();
    }

    protected void sendNoContent(HttpExchange ex) throws java.io.IOException {
        ex.sendResponseHeaders(HttpStatus.NO_CONTENT.getCode(), -1);
        ex.close();
    }
}