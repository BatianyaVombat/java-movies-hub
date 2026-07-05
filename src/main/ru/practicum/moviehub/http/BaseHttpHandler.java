package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

abstract class BaseHttpHandler implements HttpHandler {
    protected static final String CT_JSON = "application/json; charset=UTF-8"; // !!! Укажите содержимое заголовка Content-Type

    protected void sendJson(HttpExchange ex, int status, String json) throws IOException {
        // !!! Реализуйте общий для всех хендлеров метод
        // для отправки ответа с телом в формате JSON
        ex.getResponseHeaders().set("Content-Type", CT_JSON);
        byte[] bytes = json.getBytes(StandardCharsets.UTF_8);
        ex.sendResponseHeaders(status, bytes.length);

        try (OutputStream os = ex.getResponseBody()) {
            os.write(bytes);
        }
    }

    protected void sendNoContent(HttpExchange ex, int status) throws IOException {
        // !!! Реализуйте общий для всех хендлеров метод
        // для отправки ответа без тела и кодом 204
        ex.sendResponseHeaders(status, -1);
    }
}