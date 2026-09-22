package lithej.net;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import lithej.core.Validate;

/**
 * Immutable options controlling how {@link Http} performs a request: extra headers,
 * extra query parameters, a per-request timeout, and (optionally) a caller-supplied
 * {@link HttpClient}.
 *
 * <p>Each {@code withX} method returns a new instance; the receiver is not modified.
 * Start from {@link #defaults()} and layer on only the options you need:
 *
 * <pre>{@code
 * HttpOptions options = HttpOptions.defaults()
 *         .withHeader("Accept", "application/json")
 *         .withQueryParam("page", "2")
 *         .withTimeout(Duration.ofSeconds(5));
 * HttpResponse response = Http.get("https://example.com/items", options);
 * }</pre>
 *
 * <p><b>Header precedence:</b> {@link #withHeader(String, String)} and
 * {@link #withHeaders(Map)} accumulate across calls; when the same header name is set
 * more than once (in one call to {@link #withHeaders(Map)}, or across several calls),
 * the last value set for that name wins. This mirrors how a {@code Map} put would
 * behave, and lets a caller build up a base set of headers and override a handful of
 * them per call.
 *
 * <p><b>Query parameter encoding:</b> {@link #withQueryParam(String, String)}
 * accumulates name/value pairs the same way. Both the name and the value are
 * percent-encoded with {@link java.net.URLEncoder#encode(String, java.nio.charset.Charset)}
 * (UTF-8, {@code application/x-www-form-urlencoded} rules, so a space becomes
 * {@code +}) when {@link Http} builds the final request URI, and are appended to any
 * query string already present in the URL passed to {@link Http}.
 *
 * <p><b>Default HTTP client:</b> when no {@link HttpClient} is supplied via
 * {@link #withClient(HttpClient)}, {@link Http} uses a single lazily-created shared
 * default client with a 10-second connect timeout and
 * {@link HttpClient.Redirect#NORMAL} redirect handling. Supply your own client to
 * control connection pooling, TLS configuration, proxying, or redirect policy.
 *
 * <p><b>Sensitive headers:</b> {@link #toString()} redacts the value of any header
 * whose name looks like it carries a secret (see {@link SensitiveHeaders}), printing
 * {@code ***} instead of the real value. {@link #headers()} is unaffected and always
 * returns the real values.
 *
 * <p><b>Thread safety:</b> immutable and thread-safe. A shared {@link HttpClient}
 * supplied via {@link #withClient(HttpClient)} is itself documented by the JDK as
 * thread-safe, so an {@code HttpOptions} instance (and the client it carries) may
 * safely be reused across concurrent {@link Http} calls.
 */
public final class HttpOptions {

    private static final String PARAM_NAME = "name";
    private static final String PARAM_VALUE = "value";
    private static final String PARAM_HEADERS = "headers";
    private static final String PARAM_TIMEOUT = "timeout";
    private static final String PARAM_CLIENT = "client";

    private static final HttpOptions DEFAULTS = new HttpOptions(Map.of(), Map.of(), null, null);

    private final Map<String, String> headers;
    private final Map<String, String> queryParams;
    private final Duration timeout;
    private final HttpClient client;

    private HttpOptions(Map<String, String> headers, Map<String, String> queryParams, Duration timeout,
            HttpClient client) {
        this.headers = headers;
        this.queryParams = queryParams;
        this.timeout = timeout;
        this.client = client;
    }

    /**
     * Returns the default options: no extra headers or query parameters, no timeout,
     * and no caller-supplied client (so {@link Http} uses its shared default client).
     *
     * @return the default options
     */
    public static HttpOptions defaults() {
        return DEFAULTS;
    }

    /**
     * Returns a copy of this options object with one additional header set.
     *
     * <p>If a header with this name was already set (directly or via
     * {@link #withHeaders(Map)}), the new value replaces it; see the class
     * documentation for the full precedence rule.
     *
     * @param name the header name, e.g. {@code "Accept"}
     * @param value the header value
     * @return a new {@code HttpOptions} with the header added
     * @throws NullPointerException if either argument is {@code null}
     */
    public HttpOptions withHeader(String name, String value) {
        Validate.notNull(name, PARAM_NAME);
        Validate.notNull(value, PARAM_VALUE);
        Map<String, String> updated = new LinkedHashMap<>(headers);
        updated.put(name, value);
        return new HttpOptions(updated, queryParams, timeout, client);
    }

    /**
     * Returns a copy of this options object with several additional headers set.
     *
     * <p>Same precedence rule as {@link #withHeader(String, String)}: for a name that
     * collides with a header already set, or that is repeated within {@code headers}
     * itself, the last value set wins.
     *
     * @param headers header names and values to add or override
     * @return a new {@code HttpOptions} with the headers added
     * @throws NullPointerException if {@code headers} is {@code null}
     */
    public HttpOptions withHeaders(Map<String, String> headers) {
        Validate.notNull(headers, PARAM_HEADERS);
        Map<String, String> updated = new LinkedHashMap<>(this.headers);
        updated.putAll(headers);
        return new HttpOptions(updated, queryParams, timeout, client);
    }

    /**
     * Returns a copy of this options object with one additional query parameter, to
     * be percent-encoded and appended to the request URL's query string. See the class
     * documentation for the encoding rule and precedence when a name is repeated.
     *
     * @param name the query parameter name
     * @param value the query parameter value
     * @return a new {@code HttpOptions} with the query parameter added
     * @throws NullPointerException if either argument is {@code null}
     */
    public HttpOptions withQueryParam(String name, String value) {
        Validate.notNull(name, PARAM_NAME);
        Validate.notNull(value, PARAM_VALUE);
        Map<String, String> updated = new LinkedHashMap<>(queryParams);
        updated.put(name, value);
        return new HttpOptions(headers, updated, timeout, client);
    }

    /**
     * Returns a copy of this options object with the given per-request timeout,
     * applied via {@link java.net.http.HttpRequest.Builder#timeout(Duration)}.
     *
     * @param timeout the maximum time to wait for the request to complete
     * @return a new {@code HttpOptions} with the given timeout
     * @throws NullPointerException if {@code timeout} is {@code null}
     * @throws IllegalArgumentException if {@code timeout} is zero or negative
     */
    public HttpOptions withTimeout(Duration timeout) {
        Validate.notNull(timeout, PARAM_TIMEOUT);
        Validate.isTrue(!timeout.isZero() && !timeout.isNegative(), "timeout must be positive");
        return new HttpOptions(headers, queryParams, timeout, client);
    }

    /**
     * Returns a copy of this options object that uses {@code client} instead of
     * {@link Http}'s shared default client, e.g. for connection pooling across calls,
     * a custom {@link javax.net.ssl.SSLContext}, proxy configuration, or a different
     * redirect policy.
     *
     * @param client the client to use for the request
     * @return a new {@code HttpOptions} with the given client
     * @throws NullPointerException if {@code client} is {@code null}
     */
    public HttpOptions withClient(HttpClient client) {
        Validate.notNull(client, PARAM_CLIENT);
        return new HttpOptions(headers, queryParams, timeout, client);
    }

    /**
     * Returns the configured extra headers.
     *
     * @return an unmodifiable copy of the configured headers; never {@code null},
     *     possibly empty
     */
    public Map<String, String> headers() {
        return Map.copyOf(headers);
    }

    /**
     * Returns the configured extra query parameters.
     *
     * @return an unmodifiable copy of the configured query parameters, in their
     *     un-encoded form; never {@code null}, possibly empty
     */
    public Map<String, String> queryParams() {
        return Map.copyOf(queryParams);
    }

    /**
     * Returns the configured per-request timeout.
     *
     * @return the timeout, or {@code null} if none is set
     */
    public Duration timeout() {
        return timeout;
    }

    /**
     * Returns the configured client.
     *
     * @return the caller-supplied client, or {@code null} to use {@link Http}'s
     *     shared default client
     */
    public HttpClient client() {
        return client;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof HttpOptions other)) {
            return false;
        }
        return headers.equals(other.headers)
                && queryParams.equals(other.queryParams)
                && Objects.equals(timeout, other.timeout)
                && Objects.equals(client, other.client);
    }

    @Override
    public int hashCode() {
        return Objects.hash(headers, queryParams, timeout, client);
    }

    @Override
    public String toString() {
        Map<String, String> displayHeaders = new LinkedHashMap<>();
        headers.forEach((name, value) ->
                displayHeaders.put(name, SensitiveHeaders.isSensitive(name) ? SensitiveHeaders.REDACTED : value));
        return "HttpOptions[headers=" + displayHeaders
                + ", queryParams=" + queryParams
                + ", timeout=" + timeout
                + ", client=" + (client != null ? "custom" : "default") + "]";
    }
}
