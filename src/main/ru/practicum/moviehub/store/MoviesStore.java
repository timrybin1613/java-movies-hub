package ru.practicum.moviehub.store;

import ru.practicum.moviehub.model.Movie;

import java.util.*;
import java.util.stream.Collectors;

public class MoviesStore {

    private Map<Integer, Movie> movies;
    private int nextId = 1;

    public MoviesStore() {
        movies = new HashMap<>();
    }

    public HashMap<Integer, Movie> getMovies() {
        return new HashMap<>(movies);
    }

    public Set<Map.Entry<Integer, Movie>> getEntries() {
        return new HashMap<>(movies).entrySet();
    }

    public int addMovie(Movie movie) {
        int id = nextId++;
        movies.put(id, movie);
        return id;
    }

    public Optional<Movie> getMovieById(int id) {
        return Optional.ofNullable(movies.get(id));
    }

    public boolean deleteMovie(int id) {
        return movies.remove(id) != null;
    }

    public Map<Integer, Movie> getMoviesByYears(List<Integer> years) {
        Set<Integer> setYears = new HashSet<>(years);

        return movies.entrySet().stream()
                .filter(e -> setYears.contains(e.getValue().getReleaseDate().getYear()))
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public void clear() {
        movies.clear();
        nextId = 1;
    }
}