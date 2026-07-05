package ru.practicum.moviehub.http;

import org.junit.jupiter.api.*;
import ru.practicum.moviehub.api.ErrorResponse;
import ru.practicum.moviehub.model.Movie;
import ru.practicum.moviehub.store.MoviesStore;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Year;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.*;
import static ru.practicum.moviehub.MovieHubApp.GSON;

public class MoviesApiTest {
    private static final String BASE = "http://localhost:8080";
    private static final String MOVIE_PATH = "/movies";
    private static final Charset DEFAULT_CHARSET = StandardCharsets.UTF_8;
    private static MoviesServer server;
    private static HttpClient client;
    private static MoviesStore store;
    private static final ListOfMoviesTypeToken listOfMoviesTypeToken = new ListOfMoviesTypeToken();

    @BeforeAll
    static void beforeAll() throws InterruptedException {
        store = new MoviesStore();
        server = new MoviesServer(store);
        server.start();

        client = HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(2)).build();
        Thread.sleep(100);
    }

    @BeforeEach
    void setUp() {
        store = server.getMovieStore();
        store.removeAllMovies();
    }

    @AfterAll
    static void afterAll() {
        server.stop();
    }

    //---------------Блок тестирования GET /movies---------------
    @Test
    @DisplayName("Проверяем GET при пустом хранилище")
    void getMovies_whenEmpty_returnsEmptyArray() throws Exception {
        // Отправьте запрос
        HttpResponse<String> resp = getResponseForGet(MOVIE_PATH);

        // Допишите проверку кода ответа
        assertEquals(200, resp.statusCode(), "GET /movies должен вернуть 200");

        // Допишите проверку заголовка Content-Type
        String contentTypeHeaderValue =
                resp.headers().firstValue("Content-Type").orElse("");
        assertEquals("application/json; charset=UTF-8",
                contentTypeHeaderValue, "Content-Type должен содержать формат данных и кодировку");

        // проверка, что был возвращён массив
        String body = resp.body().trim();
        assertTrue(body.startsWith("[") && body.endsWith("]"),
                "Ожидается JSON-массив");
    }

    @Test
    @DisplayName("Наполнили хранилище, проверяем что в теле ответа есть добавленные фильмы")
    void shouldReturnMoviesArrayWhenMoviesExist() throws IOException, InterruptedException {
        store.addMovie(new Movie("Начало", 2010));
        store.addMovie(new Movie("Интерстеллар", 2014));

        HttpResponse<String> response = getResponseForGet(MOVIE_PATH);
        assertEquals(200, response.statusCode());

        List<Movie> movies = GSON.fromJson(response.body(), listOfMoviesTypeToken.getType());
        assertEquals(2, movies.size());
        assertEquals("Начало", movies.getFirst().title());
        assertEquals(2014, movies.get(1).year());
    }

    //---------------Блок тестирования POST /movies---------------
    @Test
    @DisplayName("Добавление фильма при корректных данных")
    void shouldAddMovieWithValidData() throws IOException, InterruptedException {
        String jsonBody = "{\"title\":\"Начало\",\"year\":2010}";
        HttpResponse<String> response = postJson(jsonBody, null);

        assertEquals(201, response.statusCode());

        //проверяем что фильм точно в хранилище
        assertEquals(1, store.getAllMovies().size());

        var body = GSON.fromJson(response.body(), Map.class);
        Assertions.assertNotNull(body.get("id"));
        assertEquals("Начало", body.get("title"));
        assertEquals(2010, ((Number) body.get("year")).intValue());
    }

    @Test
    @DisplayName("Ошибка при пустом title")
    void shouldError422WhenTittleIsEmpty() throws IOException, InterruptedException {
        String jsonBody = "{\"title\":\"\",\"year\":2010}";
        HttpResponse<String> response = postJson(jsonBody, null);

        assertEquals(422, response.statusCode());

        ErrorResponse error = GSON.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", error.getError());
        assertTrue(error.getDetails().contains("название не должно быть пустым"));
    }

    @Test
    @DisplayName("Ошибка при слишком длинном поле title")
    void shouldReturn422WhenTitleTooLong() throws IOException, InterruptedException {
        String longTitle = "A".repeat(101);
        String jsonBody = String.format("{\"title\":\"%s\",\"year\":2010}", longTitle);
        HttpResponse<String> response = postJson(jsonBody, null);

        assertEquals(422, response.statusCode());
        ErrorResponse error = GSON.fromJson(response.body(), ErrorResponse.class);
        assertTrue(error.getDetails().contains("название не должно превышать 100 символов"));
    }

    @Test
    @DisplayName("Ошибка при неверном поле year")
    void shouldReturn422WhenYearOutOfRange() throws IOException, InterruptedException {
        String jsonBody = "{\"title\":\"Начало\",\"year\":1800}";
        HttpResponse<String> response = postJson(jsonBody, null);

        assertEquals(422, response.statusCode());
        ErrorResponse error = GSON.fromJson(response.body(), ErrorResponse.class);
        String expectedMessage = "год должен быть между 1888 и " + (Year.now().getValue() + 1);

        assertTrue(error.getDetails().contains(expectedMessage));
    }

    @Test
    @DisplayName("Ошибка при неправильном Content-Type")
    void shouldReturn415ForWrongContentType() throws IOException, InterruptedException {
        String jsonBody = "{\"title\":\"Начало\",\"year\":2010}";
        Map<String, String> headers = Map.of("Content-Type", "text/plain");
        HttpResponse<String> response = postJson(jsonBody, headers);

        assertEquals(415, response.statusCode());
        assertTrue(response.body().isEmpty());
    }

    @Test
    @DisplayName("Ошибка при некорректном JSON")
    void shouldReturn422ForMalformedJson() throws IOException, InterruptedException {
        String notCorrectJson = "\"title\":\"Начало\",\"year\":2010,";
        HttpResponse<String> response = postJson(notCorrectJson, null);

        assertEquals(422, response.statusCode());

        ErrorResponse error = GSON.fromJson(response.body(), ErrorResponse.class);
        assertEquals("Ошибка валидации", error.getError());
        assertTrue(error.getDetails().contains("Некорректный формат JSON"));
    }

    //---------------Блок тестирования GET /movies/{id}--------------
    @Test
    @DisplayName("Возвращаем фильм по id")
    void shouldReturnMovieById() throws IOException, InterruptedException {
        store.addMovie(new Movie("Начало", 2010));
        HttpResponse<String> response = getResponseForGet("/movies/1");

        assertEquals(200, response.statusCode());
        var body = GSON.fromJson(response.body(), Map.class);
        assertEquals("Начало", body.get("title"));
        assertEquals(2010, ((Number) body.get("year")).intValue());
        assertNotNull(body.get("id"));
    }

    @Test
    @DisplayName("Возвращаем ошибку если фильм не найден")
    void shouldReturn404WhenMovieNotFound() throws IOException, InterruptedException {
        HttpResponse<String> response = getResponseForGet("/movie/99");
        assertEquals(404, response.statusCode());
    }

    @Test
    @DisplayName("Возвращаем ошибку если id не число")
    void shouldReturn400WhenIdNotNumber() throws IOException, InterruptedException {
        HttpResponse<String> response = getResponseForGet("/movie/abs");
        assertEquals(404, response.statusCode());
    }

    //---------------Блок тестирования DELETE /movies/{id}---------------
    @Test
    @DisplayName("Успешное удаление фильма")
    void shouldDeleteMovieSuccessfully() throws IOException, InterruptedException {
        store.addMovie(new Movie("Гладиатор", 2000));
        assertEquals("Гладиатор", store.getMovies(1).get().title());

        HttpResponse<String> response = getResponseForDelete(BASE + "/movies/1");
        assertEquals(204, response.statusCode());
        assertTrue(store.getAllMovies().isEmpty());
    }

    @Test
    @DisplayName("Попытка удалить фильм, но id не существует")
    void shouldReturn404WhenDeletingNonExistentMovie() throws IOException, InterruptedException {
        HttpResponse<String> response = getResponseForDelete(BASE + "/movies/999");
        assertEquals(404, response.statusCode());
    }

    @Test
    @DisplayName("Попытка удалить фильм, но id неверного формата")
    void shouldReturn400WhenDeletingWithInvalidId() throws IOException, InterruptedException {
        HttpResponse<String> response = getResponseForDelete(BASE + "/movies/avs");
        assertEquals(400, response.statusCode());
    }

    //---------------Блок тестирования GET /movies?year=YYYY---------------
    @Test
    @DisplayName("Фильтрация по году возвращает правильные фильмы")
    void shouldFilterMoviesByYear() throws IOException, InterruptedException {
        store.addMovie(new Movie("Ярость", 2014));
        store.addMovie(new Movie("Стрингер", 2014));
        store.addMovie(new Movie("Безумная свадьба", 2014));
        store.addMovie(new Movie("Девушка с татуировкой дракона", 2011));

        HttpResponse<String> response = getResponseForGet("/movies?year=2014");
        assertEquals(200, response.statusCode());

        List<Movie> movies = GSON.fromJson(response.body(), listOfMoviesTypeToken.getType());
        assertEquals("Ярость", movies.getFirst().title());
        assertEquals("Безумная свадьба", movies.get(2).title());

        boolean match = movies.stream().noneMatch(m -> "Девушка с татуировкой дракона".equals(m.title()));
        assertTrue(match);
    }

    @Test
    @DisplayName("Фильтрация по нечисловому year возвращает 400")
    void shouldReturn400WhenYearIsNotNumber() throws IOException, InterruptedException {
        HttpResponse<String> response = getResponseForGet("/movies?title=sds");
        assertEquals(400, response.statusCode());
        assertTrue(response.body().contains("Некорректный параметр запроса"));
    }

    @Test
    @DisplayName("Фильтрация с пустым значением year возвращает 400")
    void shouldReturn400WhenYearIsEmpty() throws IOException, InterruptedException {
        HttpResponse<String> response = getResponseForGet("/movies?title=");
        assertEquals(400, response.statusCode());
    }

    @Test
    @DisplayName("Фильтрация с неверным ключом возвращает 400")
    void shouldReturn400WhenWrongQueryKey() throws IOException, InterruptedException {
        HttpResponse<String> response = getResponseForGet("/movies?title=Начало");
        assertEquals(400, response.statusCode());
    }

    //вспомогательный метод для GET
    private HttpResponse<String> getResponseForGet(String path) throws IOException, InterruptedException {
        HttpRequest req = HttpRequest.newBuilder()
                .uri(URI.create(BASE + path))
                .GET()
                .build();

        return client.send(req, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));
    }

    //вспомогательный метод для POST
    private HttpResponse<String> postJson(String jsonBody, Map<String, String> headers)
            throws IOException, InterruptedException {
        HttpRequest.Builder builder = HttpRequest.newBuilder()
                .uri(URI.create(BASE + MoviesApiTest.MOVIE_PATH))
                .POST(HttpRequest.BodyPublishers.ofString(jsonBody, DEFAULT_CHARSET));

        if (headers != null) {
            headers.forEach(builder::header);
        } else {
            builder.setHeader("Content-Type", "application/json; charset=UTF-8");
        }

        HttpRequest request = builder.build();

        return client.send(request, HttpResponse.BodyHandlers.ofString(DEFAULT_CHARSET));
    }

    //вспомогательный метод для DELETE
    private HttpResponse<String> getResponseForDelete(String path) throws IOException, InterruptedException {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create(path))
                .DELETE()
                .build();

        return client.send(request, HttpResponse.BodyHandlers.ofString());
    }
}