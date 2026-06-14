package ru.practicum.moviehub.api;

import java.util.List;

public class ErrorResponse {
    public String error;
    public List<String> details;


    public ErrorResponse(String error, List<String> details) {
        this.error = error;
        this.details = details;
    }

    public String getError() {
        return error;
    }

    public List<String> getDetails() {
        return details;
    }
}