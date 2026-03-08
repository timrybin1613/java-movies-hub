package ru.practicum.moviehub.http;

import com.google.gson.Gson;
import org.junit.jupiter.api.*;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.api.dto.MovieDto;
import ru.practicum.moviehub.json.GsonFactory;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.LocalDate;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static MoviesServer server;
    private static HttpClient client;
    private static Gson gson;

    @BeforeAll
    static void beforeAll() {
        gson = GsonFactory.get();

        server = new MoviesServer(new MoviesStore(), 8080);
        server.start();

        client = HttpClient.newBuilder()
                .connectTimeout(Duration.ofSeconds(2))
                .build();
    }

    @AfterEach
    void afterEach() {
        server.getStore().clear();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    @Test
    @DisplayName("GET /movies при пустом хранилище")
    void shouldReturnEmptyListWhenNoMoviesExist() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String body = resp.body().trim();

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
        assertJson(resp);
        assertEquals("[]", body, "Ожидается JSON-массив");
    }

    @Test
    @DisplayName("GET /movies с одним фильмом в хранилище")
    void shouldReturnAllMovies() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .GET()
                .build();

        server.getStore().addMovie(new Movie("Фильм", LocalDate.of(1999, 1, 1)));

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String body = resp.body().trim();

        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");
        assertJson(resp);
        assertEquals("[{\"id\":1,\"title\":\"Фильм\",\"releaseDate\":\"1999-01-01\"}]", body);
    }

    @Test
    @DisplayName("POST /movies позитивный сценарий")
    void shouldCreateMovieSuccessfully() throws Exception {
        Movie movie = new Movie("Фильм", LocalDate.of(1999, 1, 1));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json; charset=UTF-8")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movie)))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        Map<Integer, Movie> movies =
                server.getStore().getMovies();
        MovieDto movieDto = gson.fromJson(resp.body(), MovieDto.class);
        Movie saved = movies.values().iterator().next();

        assertEquals(201, resp.statusCode(), "POST /movies должен вернуть 201");
        assertJson(resp);
        assertEquals(movie, saved);
        assertEquals("Фильм", movieDto.getTitle());
        assertEquals(LocalDate.of(1999, 1, 1), movieDto.getReleaseDate());
        assertEquals(1, movieDto.getId());
    }

    @Test
    @DisplayName("POST /movies с пустым title")
    void shouldReturn422WhenTitleIsEmpty() throws Exception {
        Movie movie = new Movie("", LocalDate.of(2000, 2, 1));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movie)))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);

        assertEquals(422, resp.statusCode(), "POST /movies с пустым title должен вернуть 422");
        assertEquals("Ошибка валидации", error.getError());
        assertEquals("Название не должно быть пустым", error.getDetails()[0]);
        assertJson(resp);
        assertTrue(server.getStore().getMovies().isEmpty());
    }

    @Test
    @DisplayName("POST /movies с слишком длинным title")
    void shouldReturn422WhenTitleTooLong() throws Exception {
        Movie movie = new Movie("a".repeat(101), LocalDate.of(2000, 2, 1));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movie)))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);

        assertEquals(422, resp.statusCode(), "POST /movies с слишком длинным title должен вернуть 422");
        assertEquals("Ошибка валидации", error.getError());
        assertEquals("Допустимая длина названия не более 100 символов", error.getDetails()[0]);
        assertJson(resp);
        assertTrue(server.getStore().getMovies().isEmpty());
    }

    @Test
    @DisplayName("POST /movies с year < 1888")
    void shouldReturn422WhenYearLessThanAllowed() throws Exception {
        Movie movie = new Movie("Фильм", LocalDate.of(1887, 2, 1));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movie)))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);

        assertEquals(422, resp.statusCode(), "POST /movies с year < 1888 должен вернуть 422");
        assertEquals("Ошибка валидации", error.getError());
        assertEquals("год должен быть между 1888 и 2027", error.getDetails()[0]);
        assertJson(resp);
        assertTrue(server.getStore().getMovies().isEmpty());
    }

    @Test
    @DisplayName("POST /movies с year > текущий год + 1")
    void shouldReturn422WhenYearGreaterThanAllowed() throws Exception {
        Movie movie = new Movie("Фильм", LocalDate.of(LocalDate.now().getYear() + 2, 2, 1));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movie)))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);

        assertEquals(422, resp.statusCode(), "POST /movies с year > текущий год + 1 должен вернуть 422");
        assertEquals("Ошибка валидации", error.getError());
        assertEquals("год должен быть между 1888 и 2027", error.getDetails()[0]);
        assertJson(resp);
        assertTrue(server.getStore().getMovies().isEmpty());
    }

    @Test
    @DisplayName("POST /movies с Content-Type != application/json")
    void shouldReturn415WhenContentTypeIsNotJson() throws Exception {
        Movie movie = new Movie("Фильм", LocalDate.of(2025, 2, 1));
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "text/plain")
                .POST(HttpRequest.BodyPublishers.ofString(gson.toJson(movie)))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);

        assertEquals(415, resp.statusCode(), "POST /movies с Content-Type отличным " +
                "от application/json должен вернуть 415");
        assertEquals("Unsupported Media Type", error.getError());
        assertTrue(server.getStore().getMovies().isEmpty());
    }

    @Test
    @DisplayName("POST /movies с невалидным json")
    void shouldReturn422WhenRequestBodyIsInvalidJson() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString("sdsad"))
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        ErrorResponse error = gson.fromJson(resp.body(), ErrorResponse.class);

        assertEquals(422, resp.statusCode(), "POST /movies с невалидным json телом должен вернуть 422");
        assertEquals("Некорректное тело запроса", error.getError());
        assertTrue(server.getStore().getMovies().isEmpty());
    }

    @Test
    @DisplayName("GET /movies/{id} с валидным id")
    void shouldReturnMovieById() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .GET()
                .build();

        server.getStore().addMovie(new Movie("Фильм1", LocalDate.of(1998, 1, 1)));

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(200, resp.statusCode(), "GET /movies/{id} должен вернуть 200");

        assertJson(resp);

        String body = resp.body().trim();
        assertEquals("{\"id\":1,\"title\":\"Фильм1\",\"releaseDate\":\"1998-01-01\"}", body);
    }

    @Test
    @DisplayName("GET /movies/{несуществующий id}")
    void shouldReturn404WhenMovieNotFoundById() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/2"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode(), "GET /movies/{несуществующий id} должен вернуть 404");
    }

    @Test
    @DisplayName("GET /movies/{некорректный id}")
    void shouldReturn400WhenMovieIdIsInvalid() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/a"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "GET /movies/{некорректный id} должен вернуть 400");
    }

    @Test
    @DisplayName("DELETE /movies/{id}")
    void shouldDeleteMovieSuccessfully() throws Exception {
        server.getStore().addMovie(new Movie("Фильм1", LocalDate.of(1998, 1, 1)));

        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/1"))
                .DELETE()
                .build();

        assertFalse(server.getStore().getMovies().isEmpty());

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(204, resp.statusCode(), "DELETE /movies/{id} должен вернуть 204");
        assertTrue(server.getStore().getMovies().isEmpty());
    }

    @Test
    @DisplayName("DELETE /movies/{несуществующий id}")
    void shouldReturn404WhenDeletingNonExistingMovie() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/2"))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(404, resp.statusCode(), "DELETE /movies/{несуществующий id} должен вернуть 404");
    }

    @Test
    @DisplayName("DELETE /movies/{некорректный id}")
    void shouldReturn400WhenDeletingWithInvalidId() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies/a"))
                .DELETE()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));

        assertEquals(400, resp.statusCode(), "DELETE /movies/{некорректный id} должен вернуть 400");
    }

    @Test
    @DisplayName("GET /movies?&year={year}")
    void shouldFilterMoviesByYear() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=1999"))
                .GET()
                .build();

        server.getStore().addMovie(new Movie("Фильм1", LocalDate.of(1999, 1, 1)));
        server.getStore().addMovie(new Movie("Фильм2", LocalDate.of(1994, 2, 1)));
        server.getStore().addMovie(new Movie("Фильм3", LocalDate.of(1999, 1, 1)));

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String body = resp.body().trim();

        assertEquals(200, resp.statusCode());
        assertJson(resp);
        assertEquals("[{\"id\":1,\"title\":\"Фильм1\",\"releaseDate\":\"1999-01-01\"}," +
                "{\"id\":3,\"title\":\"Фильм3\",\"releaseDate\":\"1999-01-01\"}]", body);
    }

    @Test
    @DisplayName("GET /movies?year={year нет в хранилище}")
    void shouldReturnEmptyListWhenNoMoviesMatchYear() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=2000"))
                .GET()
                .build();

        server.getStore().addMovie(new Movie("Фильм1", LocalDate.of(1999, 1, 1)));
        server.getStore().addMovie(new Movie("Фильм2", LocalDate.of(1994, 2, 1)));
        server.getStore().addMovie(new Movie("Фильм3", LocalDate.of(1999, 1, 1)));

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String body = resp.body().trim();

        assertEquals(200, resp.statusCode());
        assertJson(resp);
        assertEquals("[]", body);
    }

    @Test
    @DisplayName("GET /movies?&year={year не число}")
    void shouldReturn400WhenYearParameterIsInvalid() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies?year=&year=adsd"))
                .GET()
                .build();

        HttpResponse<String> resp =
                client.send(req, HttpResponse.BodyHandlers.ofString(StandardCharsets.UTF_8));
        String body = resp.body().trim();

        assertEquals(400, resp.statusCode());
        assertEquals("{\"error\":\"Некорректный параметр запроса — 'year'\"}", body);
    }

    @Test
    @DisplayName("вызов неподдерживаемого метода")
    void shouldReturn405WhenMethodNotAllowed() throws Exception {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + "/movies"))
                .PUT(HttpRequest.BodyPublishers.noBody())
                .build();

        HttpResponse<String> resp =
                client.send(req,
                        HttpResponse.BodyHandlers.ofString());

        assertEquals(405, resp.statusCode());
    }

    private void assertJson(HttpResponse<?> resp) {
        assertEquals("application/json; charset=UTF-8", resp.headers().firstValue("Content-Type").orElse("")
        );
    }

}