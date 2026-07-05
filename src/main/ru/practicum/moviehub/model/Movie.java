package ru.practicum.moviehub.model;

public record Movie(String title, int year) {
    public Movie {
        if (title == null || title.isEmpty() || title.length() > 100) {
            throw new IllegalArgumentException("Некорректное название");
        }

        if (year < 1888 || year > 2026) {
            throw new IllegalArgumentException("Год должен быть в пределах от 1888 до 2026");
        }

    }
}