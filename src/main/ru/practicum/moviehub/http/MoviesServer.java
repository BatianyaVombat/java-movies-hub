package ru.practicum.moviehub.http;

import com.sun.net.httpserver.HttpServer;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.InetSocketAddress;

public class MoviesServer {
    private static final String MOVIE_PATH = "/movies";
    private final HttpServer server;
    private final MoviesStore store;

    public MoviesServer(MoviesStore store) {
        try {
            // создайте сервер
            this.store = store;
            server = HttpServer.create(new InetSocketAddress(8080), 0);
            server.createContext(MOVIE_PATH, new MoviesHandler(store));
        } catch (IOException e) {
            throw new RuntimeException("Не удалось создать HTTP-сервер", e);
        }
    }

    public void start() {
        // запустите сервер
        server.start();
        System.out.println("Сервер запущен");
    }

    public void stop() {
        // остановите сервер
        server.stop(0);
        System.out.println("Сервер остановлен");
    }

    public MoviesStore getMovieStore() {
        return store;
    }
}