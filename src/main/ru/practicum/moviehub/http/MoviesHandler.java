package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpHandler;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.util.*;

class MoviesHandler extends BaseHttpHandler implements HttpHandler {
    private final HttpHandler getHandler;
    private final HttpHandler postHandler;
    private final HttpHandler deleteHandler;

    public MoviesHandler(MoviesStore store) throws IOException {
        getHandler = new GetMoviesHandler(store);
        postHandler = new PostMoviesHandler(store);
        deleteHandler = new DeleteMoviesHandler(store);
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        // Напишите реализацию, удовлетворяющую тест
        String method = ex.getRequestMethod();

        if (method.equalsIgnoreCase("GET")) {
            getHandler.handle(ex);
        } else if (method.equalsIgnoreCase("POST")) {
            postHandler.handle(ex);
        } else if (method.equalsIgnoreCase("DELETE")) {
            deleteHandler.handle(ex);
        } else {
            sendNoContent(ex, 405);
        }
    }
}