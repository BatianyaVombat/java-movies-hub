package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.util.Optional;

public class DeleteMoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;

    public DeleteMoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String[] pathPart = ex.getRequestURI().getPath().split("/");

        if (pathPart.length < 3) {
            sendJson(ex, 400, "{\"error\":\"Некорректный ID\"}");
            return;
        }

        String idString = pathPart[2];

        try {
            int id = Integer.parseInt(idString);
            Optional<Movie> deleted = store.removeMovie(id);

            if (deleted.isEmpty()) {
                sendJson(ex, 404, "{\"error\":\"Фильм не найден\"}");
                return;
            }

            sendNoContent(ex, 204);
        } catch (NumberFormatException e) {
            sendJson(ex, 400, "{\"error\":\"Некорректный ID\"}");
        }
    }
}
