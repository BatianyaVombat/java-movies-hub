package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;

import static ru.practicum.moviehub.MovieHubApp.GSON;


public class GetMoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;

    public GetMoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();
        String method = ex.getRequestMethod();

        if (method.equalsIgnoreCase("GET")) {
            String responseString = ex.getRequestURI().getQuery();

            if (responseString != null) {
                String[] params = responseString.split("=");

                if (params.length != 2 || !params[0].equals("year") || params[1].isEmpty()) {
                    sendJson(ex, 400, "{\"error\":\"Некорректный параметр запроса — 'year'\"}");
                    return;
                }

                String yearString = params[1];

                try {
                    int year = Integer.parseInt(yearString);

                    Collection<Movie> filmsByYear = store.getAllMovies().stream()
                            .filter(film -> film.year() == year)
                            .toList();

                    sendJson(ex, 200, GSON.toJson(filmsByYear));
                    return;
                } catch (NumberFormatException e) {
                    sendJson(ex, 400, "{\"error\":\"Некорректный параметр запроса — 'year'\"}");
                    return;
                }
            }
        }

        if (path.equals("/movies")) {
            Collection<Movie> films = store.getAllMovies();
            String jsonString = GSON.toJson(films);

            sendJson(ex, 200, jsonString);
        } else if (path.startsWith("/movies/")) {
            String[] pathParts = ex.getRequestURI().getPath().split("/");

            if (pathParts.length < 3) {
                sendJson(ex, 400, "{\"error\":\"Некорректный ID\"}");
                return;
            }

            String idString = pathParts[2]; // "/movies/id"
            int id;

            try {
                id = Integer.parseInt(idString);
            } catch (NumberFormatException e) {
                sendJson(ex, 400, "{\"error\":\"Некорректный ID\"}");
                return;
            }

            Optional<Movie> movieOpt = store.getMovies(id);

            if (movieOpt.isEmpty()) {
                sendJson(ex, 404, "{\"error\":\"Фильм не найден\"}");
                return;
            }

            Movie movie = movieOpt.get();
            Map<String, Object> response = new LinkedHashMap<>();
            response.put("id", id);
            response.put("title", movie.title());
            response.put("year", movie.year());
            String json = GSON.toJson(response);

            sendJson(ex, 200, json);
        } else {
            sendNoContent(ex, 404);
        }
    }
}
