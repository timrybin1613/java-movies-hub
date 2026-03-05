package ru.practicum.moviehub.api.dto;

import ru.practicum.moviehub.model.Movie;

import java.time.LocalDate;

public class MovieDto {
    private int id;
    private String title;
    private String releaseDate;

    private MovieDto(int id, String title, LocalDate releaseDate) {
        this.id = id;
        this.title = title;
        this.releaseDate = releaseDate.toString();
    }

    public static MovieDto from(int id, Movie movie) {
        return new MovieDto(id, movie.getTitle(), movie.getReleaseDate());
    }

    public String getTitle() {
        return title;
    }

    public LocalDate getReleaseDate() {
        return LocalDate.parse(releaseDate);
    }

    public int getId() {
        return id;
    }
}
