package lithej.net;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLEncoder;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.function.UnaryOperator;
import java.util.stream.Collectors;
import lithej.core.Validate;

/**
 * A static facade over {@link HttpClient} for common GET, POST, PUT, and DELETE
 * calls.
 *
 * <p>{@code Http} is deliberately not a competing HTTP framework: it does not retry
 * requests, follow a custom redirect policy of its own, manage cookies, or guess a
 * request's content type. It builds one {@link HttpRequest} per call from a URL, an
 * optional body, and {@link HttpOptions}, sends it with an {@link HttpClient} (either
 * a shared default one, or one supplied via {@link HttpOptions#withClient(HttpClient)}),
 * and returns an {@link HttpResponse} that also exposes the raw JDK response.
 *
 * <pre>{@code
 * HttpResponse response = Http.get("https://example.com/status");
 * if (response.isSuccessful()) {
 *     System.out.println(response.body());
 * }
 *
 * HttpResponse created = Http.post(
 *         "https://example.com/items",
 *         "{\"name\":\"widget\"}",
 *         HttpOptions.defaults().withHeader("Content-Type", "application/json"));
 * }</pre>
 *
 * <p><b>Content type:</b> {@link #post(String, String)} and {@link #put(String, String)}
 * send the given body exactly as given, with no {@code Content-Type} header set
 * beyond what the caller configures via {@link HttpOptions#withHeader(String, String)}.
 * {@code Http} does not sniff or guess the body's content type; set it explicitly if
 * it matters to the server.
 *
 * <p><b>Thread safety:</b> this class is stateless and thread-safe. The shared
 * default {@link HttpClient} it lazily creates is thread-safe (per the JDK's own
 * documentation for {@link HttpClient}) and is reused across all calls that do not
 * supply their own client via {@link HttpOptions#withClient(HttpClient)}.
 */
public final class Http {

    private static final String PARAM_URL = "url";
    private static final String PARAM_OPTIONS = "options";
    private static final String PARAM_BODY = "body";

    private static final Duration DEFAULT_CONNECT_TIMEOUT = Duration.ofSeconds(10);

    private Http() {
    }

    /**
     * Performs a GET request with default options.
     *
     * @param url the request URL
     * @return the response
     * @throws NullPointerException if {@code url} is {@code null}
     * @throws IllegalArgumentException if {@code url} is not a valid URI
     * @throws HttpRequestException if the request could not be completed (connection
     *     failure, timeout, or interruption)
     */
    public static HttpResponse get(String url) {
        return get(url, HttpOptions.defaults());
    }

    /**
     * Performs a GET request with the given options.
     *
     * @param url the request URL
     * @param options headers, query parameters, timeout, and client to use
     * @return the response
     * @throws NullPointerException if either argument is {@code null}
     * @throws IllegalArgumentException if {@code url} is not a valid URI
     * @throws HttpRequestException if the request could not be completed (connection
     *     failure, timeout, or interruption)
     */
    public static HttpResponse get(String url, HttpOptions options) {
        Validate.notNull(url, PARAM_URL);
        Validate.notNull(options, PARAM_OPTIONS);
        return exchange(url, options, HttpRequest.Builder::GET);
    }

    /**
     * Performs a POST request with default options.
     *
     * <p>See the class documentation for the content-type policy: {@code body} is
     * sent as-is, with no {@code Content-Type} guessed or set.
     *
     * @param url the request URL
     * @param body the request body
     * @return the response
     * @throws NullPointerException if either argument is {@code null}
     * @throws IllegalArgumentException if {@code url} is not a valid URI
     * @throws HttpRequestException if the request could not be completed (connection
     *     failure, timeout, or interruption)
     */
    public static HttpResponse post(String url, String body) {
        return post(url, body, HttpOptions.defaults());
    }

    /**
     * Performs a POST request with the given options.
     *
     * <p>See the class documentation for the content-type policy: {@code body} is
     * sent as-is, with no {@code Content-Type} guessed or set beyond what
     * {@code options} configures.
     *
     * @param url the request URL
     * @param body the request body
     * @param options headers, query parameters, timeout, and client to use
     * @return the response
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code url} is not a valid URI
     * @throws HttpRequestException if the request could not be completed (connection
     *     failure, timeout, or interruption)
     */
    public static HttpResponse post(String url, String body, HttpOptions options) {
        Validate.notNull(url, PARAM_URL);
        Validate.notNull(body, PARAM_BODY);
        Validate.notNull(options, PARAM_OPTIONS);
        return exchange(url, options, builder -> builder.POST(bodyPublisher(body)));
    }

    /**
     * Performs a PUT request with default options.
     *
     * <p>See the class documentation for the content-type policy: {@code body} is
     * sent as-is, with no {@code Content-Type} guessed or set.
     *
     * @param url the request URL
     * @param body the request body
     * @return the response
     * @throws NullPointerException if either argument is {@code null}
     * @throws IllegalArgumentException if {@code url} is not a valid URI
     * @throws HttpRequestException if the request could not be completed (connection
     *     failure, timeout, or interruption)
     */
    public static HttpResponse put(String url, String body) {
        return put(url, body, HttpOptions.defaults());
    }

    /**
     * Performs a PUT request with the given options.
     *
     * <p>See the class documentation for the content-type policy: {@code body} is
     * sent as-is, with no {@code Content-Type} guessed or set beyond what
     * {@code options} configures.
     *
     * @param url the request URL
     * @param body the request body
     * @param options headers, query parameters, timeout, and client to use
     * @return the response
     * @throws NullPointerException if any argument is {@code null}
     * @throws IllegalArgumentException if {@code url} is not a valid URI
     * @throws HttpRequestException if the request could not be completed (connection
     *     failure, timeout, or interruption)
     */
    public static HttpResponse put(String url, String body, HttpOptions options) {
        Validate.notNull(url, PARAM_URL);
        Validate.notNull(body, PARAM_BODY);
        Validate.notNull(options, PARAM_OPTIONS);
        return exchange(url, options, builder -> builder.PUT(bodyPublisher(body)));
    }

    /**
     * Performs a DELETE request with default options.
     *
     * @param url the request URL
     * @return the response
     * @throws NullPointerException if {@code url} is {@code null}
     * @throws IllegalArgumentException if {@code url} is not a valid URI
     * @throws HttpRequestException if the request could not be completed (connection
     *     failure, timeout, or interruption)
     */
    public static HttpResponse delete(String url) {
        return delete(url, HttpOptions.defaults());
    }

    /**
     * Performs a DELETE request with the given options.
     *
     * @param url the request URL
     * @param options headers, query parameters, timeout, and client to use
     * @return the response
     * @throws NullPointerException if either argument is {@code null}
     * @throws IllegalArgumentException if {@code url} is not a valid URI
     * @throws HttpRequestException if the request could not be completed (connection
     *     failure, timeout, or interruption)
     */
    public static HttpResponse delete(String url, HttpOptions options) {
        Validate.notNull(url, PARAM_URL);
        Validate.notNull(options, PARAM_OPTIONS);
        return exchange(url, options, HttpRequest.Builder::DELETE);
    }

    private static HttpRequest.BodyPublisher bodyPublisher(String body) {
        return HttpRequest.BodyPublishers.ofString(body, StandardCharsets.UTF_8);
    }

    private static HttpResponse exchange(String url, HttpOptions options,
            UnaryOperator<HttpRequest.Builder> configureMethod) {
        URI uri = buildUri(url, options.queryParams());
        HttpRequest.Builder builder = HttpRequest.newBuilder(uri);
        options.headers().forEach(builder::header);
        if (options.timeout() != null) {
            builder.timeout(options.timeout());
        }
        configureMethod.apply(builder);

        HttpClient client = options.client() != null ? options.client() : DefaultClientHolder.INSTANCE;
        return new HttpResponse(execute(client, builder.build()));
    }

    private static java.net.http.HttpResponse<byte[]> execute(HttpClient client, HttpRequest request) {
        try {
            return client.send(request, java.net.http.HttpResponse.BodyHandlers.ofByteArray());
        } catch (IOException e) {
            throw new HttpRequestException("Request failed: " + request.method() + " " + request.uri(), e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new HttpRequestException(
                    "Interrupted while waiting for response: " + request.method() + " " + request.uri(), e);
        }
    }

    private static URI buildUri(String url, Map<String, String> queryParams) {
        URI parsed = parseUri(url);
        if (queryParams.isEmpty()) {
            return parsed;
        }
        String encodedQuery = queryParams.entrySet().stream()
                .map(entry -> encode(entry.getKey()) + "=" + encode(entry.getValue()))
                .collect(Collectors.joining("&"));
        String separator = url.indexOf('?') >= 0 ? "&" : "?";
        return parseUri(url + separator + encodedQuery);
    }

    private static URI parseUri(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException("Invalid URL: " + url, e);
        }
    }

    private static String encode(String value) {
        return URLEncoder.encode(value, StandardCharsets.UTF_8);
    }

    /**
     * Lazily initializes the shared default {@link HttpClient}, relying on the JVM's
     * class-initialization guarantees for thread-safe, exactly-once construction
     * instead of explicit locking.
     */
    private static final class DefaultClientHolder {
        static final HttpClient INSTANCE = HttpClient.newBuilder()
                .connectTimeout(DEFAULT_CONNECT_TIMEOUT)
                .followRedirects(HttpClient.Redirect.NORMAL)
                .build();

        private DefaultClientHolder() {
        }
    }
}
