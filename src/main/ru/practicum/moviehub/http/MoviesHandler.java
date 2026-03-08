package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import com.google.gson.JsonSyntaxException;
import com.sun.net.httpserver.HttpExchange;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.api.dto.MovieDto;
import ru.practicum.moviehub.http.request.RequestContext;
import ru.practicum.moviehub.http.request.RequestParser;
import ru.practicum.moviehub.json.GsonFactory;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.*;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.stream.Collectors;

public class MoviesHandler extends BaseHttpHandler {
    private final MoviesStore store;
    private static final Gson gson = GsonFactory.get();
    private static final Logger log =
            Logger.getLogger(MoviesHandler.class.getName());
    private static final LocalDate MIN_YEAR = LocalDate.of(1888, 1, 1);
    private static final LocalDate MAX_YEAR = LocalDate.now().plusYears(1);
    private static final String YEAR_QUERY_PARAM = "year";
    private static final int MAX_LENGTH_FOR_TITLE = 100;
    private static final String FILM_NOT_FOUND_ERROR_TEXT = "Фильм не найден";
    private static final String INCORRECT_ID_ERROR_TEXT = "Некорректный ID";
    private static final String INCORRECT_PARAMS_FOR_YEARS_ERROR_TEXT = "Некорректный параметр запроса — 'year'";
    private static final String UNSUPPORTED_MEDIA_TYPE_ERROR_TEXT = "Unsupported Media Type";
    private static final String EMPTY_TITLE_ERROR_TEXT = "Название не должно быть пустым";
    private static final String INCORRECT_REQUEST_BODY_ERROR_TEXT = "Некорректное тело запроса";
    private static final String MAX_LENGTH_TITLE_ERROR_TEXT =
            "Допустимая длина названия не более " + MAX_LENGTH_FOR_TITLE + " символов";
    private static final String INCORRECT_DATA_FORMAT_ERROR_TEXT = "Неверный формат даты";
    private static final String INCORRECT_YEAR_INTERVAL_ERROR_TEXT =
            "год должен быть между " + MIN_YEAR.getYear() + " и " + MAX_YEAR.getYear();
    private static final String VALIDATION_ERROR_TEXT = "Ошибка валидации";

    public MoviesHandler(MoviesStore store) {
        this.store = store;
    }

    @Override
    public void handle(HttpExchange ex) throws IOException {
        RequestContext requestContext = RequestParser.parse(ex);
        String method = requestContext.getMethod();

        try {
            switch (method) {
                case "GET":
                    handleGet(ex, requestContext);
                    return;
                case "POST":
                    handlePost(ex);
                    return;
                case "DELETE":
                    handleDelete(ex, requestContext);
                    return;
                default:
                    send(ex, HttpStatus.METHOD_NOT_ALLOWED.getCode());
                    break;
            }
        } catch (Exception e) {
            log.log(Level.SEVERE, "Unexpected error while handling request", e);
            ex.sendResponseHeaders(HttpStatus.INTERNAL_SERVER_ERROR.getCode(), -1);
        }
    }

    private void handleGet(HttpExchange ex, RequestContext context) throws IOException {
        Map<String, List<String>> queryParams;
        queryParams = context.getQueryParams();
        List<String> pathSegments = context.getPathSegments();
        int pathLength = pathSegments.size();

        if (queryParams.isEmpty()) {
            switch (pathLength) {
                case 1:
                    handleGetAllMovies(ex);
                    return;
                case 2:
                    handleGetMovieById(ex, pathSegments);
                    return;
            }
        }

        if (queryParams.containsKey(YEAR_QUERY_PARAM)) {
            handleGetMoviesByYear(ex, context);
        } else {
            send(ex, HttpStatus.BAD_REQUEST.getCode());
        }
    }

    private void handleGetAllMovies(HttpExchange ex) throws IOException {
        Set<Map.Entry<Integer, Movie>> entries = store.getEntries();
        List<MovieDto> dto = entries.stream()
                .map(e -> MovieDto.from(e.getKey(), e.getValue())).collect(Collectors.toList());
        sendJson(ex, HttpStatus.OK.getCode(), gson.toJson(dto));
    }

    private void handleGetMovieById(HttpExchange ex, List<String> pathSegments) throws IOException {
        ErrorResponse error;
        String pathId = pathSegments.get(1);
        Optional<Integer> optId = parseId(pathId);

        if (optId.isPresent()) {
            int id = optId.get();
            Optional<Movie> movie = store.getMovieById(id);
            if (movie.isPresent()) {
                MovieDto movieDto = MovieDto.from(id, movie.get());
                sendJson(ex, HttpStatus.OK.getCode(), gson.toJson(movieDto));
            } else {
                error = new ErrorResponse(FILM_NOT_FOUND_ERROR_TEXT);
                sendJson(ex, HttpStatus.NOT_FOUND.getCode(), gson.toJson(error));
            }
        } else {
            error = new ErrorResponse(INCORRECT_ID_ERROR_TEXT);
            sendJson(ex, HttpStatus.BAD_REQUEST.getCode(), gson.toJson(error));
        }
    }

    private void handleGetMoviesByYear(HttpExchange ex, RequestContext context) throws IOException {
        Map<String, List<String>> queryParams;
        queryParams = context.getQueryParams();
        ErrorResponse error;

        List<String> years = queryParams.get(YEAR_QUERY_PARAM)
                .stream().filter(y -> !y.isBlank()).collect(Collectors.toList());
        List<Integer> yearList;

        if (years.isEmpty()) {
            error = new ErrorResponse(INCORRECT_PARAMS_FOR_YEARS_ERROR_TEXT);
            sendJson(ex, HttpStatus.BAD_REQUEST.getCode(), gson.toJson(error));
            return;
        }

        try {
            yearList = years.stream().mapToInt(Integer::parseInt)
                    .boxed()
                    .collect(Collectors.toList());
        } catch (NumberFormatException e) {
            error = new ErrorResponse(INCORRECT_PARAMS_FOR_YEARS_ERROR_TEXT);
            sendJson(ex, HttpStatus.BAD_REQUEST.getCode(), gson.toJson(error));
            return;
        }

        Map<Integer, Movie> movies = store.getMoviesByYears(yearList);
        List<MovieDto> movieDto = movies.entrySet().stream()
                .map(e -> MovieDto.from(e.getKey(), e.getValue())).collect(Collectors.toList());
        sendJson(ex, HttpStatus.OK.getCode(), gson.toJson(movieDto));
    }

    private void handlePost(HttpExchange ex) throws IOException {
        InputStream is = ex.getRequestBody();
        Movie movie;
        ArrayList<String> errorDetails = new ArrayList<>();
        String body = new String(is.readAllBytes(), StandardCharsets.UTF_8);
        String reqContentType = ex.getRequestHeaders().getFirst("Content-Type");
        ErrorResponse error;

        if (reqContentType == null
                || reqContentType.isBlank()
                || !reqContentType.contains("application/json")) {
            error = new ErrorResponse(UNSUPPORTED_MEDIA_TYPE_ERROR_TEXT);
            sendJson(ex, HttpStatus.UNSUPPORTED_MEDIA_TYPE.getCode(), gson.toJson(error));
            return;
        }

        try {
            movie = gson.fromJson(body, Movie.class);
        } catch (JsonSyntaxException e) {
            error = new ErrorResponse(INCORRECT_REQUEST_BODY_ERROR_TEXT);
            sendJson(ex, HttpStatus.UNPROCESSABLE_ENTITY.getCode(), gson.toJson(error));
            return;
        }

        if (movie.getTitle() == null || movie.getTitle().isBlank()) {
            errorDetails.add(EMPTY_TITLE_ERROR_TEXT);
        } else if (movie.getTitle().length() > MAX_LENGTH_FOR_TITLE) {
            errorDetails.add(MAX_LENGTH_TITLE_ERROR_TEXT);
        }

        if (movie.getReleaseDate() == null) {
            errorDetails.add(INCORRECT_DATA_FORMAT_ERROR_TEXT);
        } else {
            LocalDate m = movie.getReleaseDate();
            if (m.isBefore(MIN_YEAR) || m.isAfter(MAX_YEAR)) {
                errorDetails.add(INCORRECT_YEAR_INTERVAL_ERROR_TEXT);
            }
        }

        if (errorDetails.isEmpty()) {
            int id = store.addMovie(movie);
            MovieDto movieDto = MovieDto.from(id, movie);
            sendJson(ex, HttpStatus.CREATED.getCode(), gson.toJson(movieDto));
        } else {
            sendJson(ex, HttpStatus.UNPROCESSABLE_ENTITY.getCode(),
                    gson.toJson(new ErrorResponse(VALIDATION_ERROR_TEXT, errorDetails.toArray(new String[0]))));
        }
    }

    private void handleDelete(HttpExchange ex, RequestContext context) throws IOException {
        ErrorResponse error;
        List<String> pathSegments = context.getPathSegments();
        int pathLength = pathSegments.size();

        if (pathLength == 2) {
            String pathId = pathSegments.get(1);
            Optional<Integer> optId = parseId(pathId);

            if (optId.isPresent()) {
                int id = optId.get();

                boolean del = store.deleteMovie(id);
                if (del) {
                    sendNoContent(ex);
                } else {
                    error = new ErrorResponse(FILM_NOT_FOUND_ERROR_TEXT);
                    sendJson(ex, HttpStatus.NOT_FOUND.getCode(), gson.toJson(error));
                }
            } else {
                error = new ErrorResponse(INCORRECT_ID_ERROR_TEXT);
                sendJson(ex, HttpStatus.BAD_REQUEST.getCode(), gson.toJson(error));
            }
        } else {
            send(ex, HttpStatus.NOT_FOUND.getCode());
        }
    }

    private Optional<Integer> parseId(String id) {
        try {
            return Optional.of(Integer.parseInt(id));
        } catch (NumberFormatException e) {
            return Optional.empty();
        }
    }
}