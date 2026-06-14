package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.Collection;
import java.util.HashMap;
import java.util.Optional;

public class MoviesStore {
    private final HashMap<Integer, Movie> movies;
    private int nextId = 1; //поле-счётчик

    public MoviesStore() {
        movies = new HashMap<>();
    }

    public int addMovie(Movie movie) {
        int id = nextId;
        nextId++;

        movies.put(id, movie);
        return id;
    }

    public Optional<Movie> removeMovie(int id) {
        return Optional.ofNullable(movies.remove(id));
    }

    public Optional<Movie> getMovies(int id) {
        return Optional.ofNullable(movies.get(id));
    }

    public Collection<Movie> getAllMovies() {
        return movies.values();
    }

    public void removeAllMovies() {
        movies.clear();
        nextId = 1;
    }
}