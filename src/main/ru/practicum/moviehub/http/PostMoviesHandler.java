package ru.practicum.moviehub.http;

import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.model.MovieRequest;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static ru.practicum.moviehub.MovieHubApp.GSON;


public class PostMoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;

    public PostMoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        String path = ex.getRequestURI().getPath();

        if (path.equals("/movies")) {
            String headerType = ex.getRequestHeaders().getFirst("Content-Type");

            if (headerType == null || !headerType.startsWith("application/json")) {
                sendNoContent(ex, 415);
                return;
            }

            String body = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
            try {
                MovieRequest request = GSON.fromJson(body, MovieRequest.class);
                List<String> listOfErrors = new ArrayList<>();

                if (request.title == null || request.title.isEmpty()) {
                    listOfErrors.add("название не должно быть пустым");
                } else if (request.title.length() > 100) {
                    listOfErrors.add("название не должно превышать 100 символов");
                }

                if (request.year < 1888 || request.year > (LocalDate.now().getYear() + 1)) {
                    listOfErrors.add("год должен быть между 1888 и " + (LocalDate.now().getYear() + 1));
                }

                if (!listOfErrors.isEmpty()) {
                    ErrorResponse error = new ErrorResponse("Ошибка валидации", listOfErrors);
                    sendJson(ex, 422, GSON.toJson(error));
                    return;
                }

                Movie film = new Movie(request.title, request.year);
                int id = store.addMovie(film);

                //"упаковка" для JSON
                Map<String, Object> response = new LinkedHashMap<>();
                response.put("id", id);
                response.put("title", film.title());
                response.put("year", film.year());
                String jsonResponse = GSON.toJson(response);
                sendJson(ex, 201, jsonResponse);

            } catch (JsonSyntaxException e) {
                ErrorResponse error = new ErrorResponse("Ошибка валидации", List.of("Некорректный формат JSON"));
                sendJson(ex, 422, GSON.toJson(error));

            }
        }
    }
}