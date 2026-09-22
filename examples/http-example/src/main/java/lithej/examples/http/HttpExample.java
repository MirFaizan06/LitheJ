package lithej.examples.http;

import java.time.Duration;
import lithej.net.Http;
import lithej.net.HttpOptions;
import lithej.net.HttpResponse;

/**
 * Demonstrates {@link Http}: a GET request against a public test API, with a timeout
 * and a custom header set via {@link HttpOptions}.
 *
 * <p>Requires internet access to run (it calls a real public API,
 * <a href="https://jsonplaceholder.typicode.com">jsonplaceholder.typicode.com</a>).
 */
public final class HttpExample {

    private HttpExample() {
    }

    public static void main(String[] args) {
        HttpOptions options = HttpOptions.defaults()
                .withHeader("Accept", "application/json")
                .withTimeout(Duration.ofSeconds(10));

        HttpResponse response = Http.get("https://jsonplaceholder.typicode.com/todos/1", options);

        System.out.println("Status: " + response.statusCode());
        System.out.println("Successful: " + response.isSuccessful());
        System.out.println("Body: " + response.body());

        if (!response.isSuccessful()) {
            throw new IllegalStateException("Unexpected status: " + response.statusCode());
        }
    }
}
