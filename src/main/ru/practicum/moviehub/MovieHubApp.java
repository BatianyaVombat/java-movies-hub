package ru.practicum.moviehub;

import com.google.gson.Gson;
import ru.practicum.moviehub.http.MoviesServer;
import ru.practicum.moviehub.store.MoviesStore;

public class MovieHubApp {
    public static final Gson GSON = new Gson();

    public static void main(String[] args) {
        final MoviesServer server = new MoviesServer(new MoviesStore());
        Runtime.getRuntime().addShutdownHook(new Thread(server::stop));
        server.start();
    }
}