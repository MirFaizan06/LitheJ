package lithej.demos.apiclient;

import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lithej.async.Async;
import lithej.config.Config;
import lithej.core.Result;
import lithej.io.FileIO;
import lithej.net.Http;
import lithej.net.HttpRequestException;
import lithej.net.HttpResponse;
import lithej.text.Text;

/**
 * Fetches several user records from a public JSON API concurrently, wraps each
 * outcome as a {@link Result} so partial failures don't abort the whole run, and
 * saves a CSV summary.
 *
 * <p>Demonstrates: {@code lithej.config} (base URL, overridable via
 * {@code -Dapi.baseUrl=...}), {@code lithej.async} (concurrent fetches on a bounded
 * executor), {@code lithej.core.Result} (per-request success/failure without
 * exceptions), {@code lithej.net} (the HTTP calls), and {@code lithej.io} (saving the
 * report) — built entirely on LitheJ's public API.
 *
 * <p>This demo does a deliberately minimal, naive extraction of the {@code "name"}
 * field from each JSON response using {@link Text#between}, to avoid adding a JSON
 * library dependency just for a demo. It is not a general-purpose JSON parser; real
 * applications should use a proper JSON library for anything beyond this
 * illustrative use.
 */
public final class HttpApiClient {

    private static final int USER_COUNT = 5;

    private HttpApiClient() {
    }

    public static void main(String[] args) {
        Config config = Config.empty()
                .withSystemProperties()
                .withDefault("api.baseUrl", "https://jsonplaceholder.typicode.com");
        String baseUrl = config.require("api.baseUrl");

        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<CompletableFuture<Result<String, String>>> futures = new ArrayList<>();
            for (int id = 1; id <= USER_COUNT; id++) {
                int userId = id;
                futures.add(Async.run(() -> fetchUserSummary(baseUrl, userId), executor));
            }

            List<Result<String, String>> results =
                    Async.await(Async.all(futures), Duration.ofSeconds(30));

            printAndSave(results);
        } finally {
            executor.shutdown();
        }
    }

    private static Result<String, String> fetchUserSummary(String baseUrl, int userId) {
        try {
            HttpResponse response = Http.get(baseUrl + "/users/" + userId);
            if (!response.isSuccessful()) {
                return Result.failure("user " + userId + ": HTTP " + response.statusCode());
            }
            String body = response.body();
            String name = Text.between(body, "\"name\": \"", "\"").orElse("(unknown)");
            String email = Text.between(body, "\"email\": \"", "\"").orElse("(unknown)");
            return Result.success(userId + "," + name + "," + email);
        } catch (HttpRequestException e) {
            return Result.failure("user " + userId + ": " + e.getMessage());
        }
    }

    private static void printAndSave(List<Result<String, String>> results) {
        List<String> lines = new ArrayList<>();
        lines.add("id,name,email");

        int succeeded = 0;
        for (Result<String, String> result : results) {
            if (result.isSuccess()) {
                succeeded++;
                lines.add(result.get());
                System.out.println("OK   " + result.get());
            } else {
                System.out.println("FAIL " + result.getError());
            }
        }

        System.out.println();
        System.out.println(succeeded + " / " + results.size() + " requests succeeded.");

        if (succeeded > 0) {
            Path output = Path.of("users-report.csv");
            FileIO.writeLines(output, lines);
            System.out.println("Saved report to " + output.toAbsolutePath());
        }
    }
}
