package lithej.net;

import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lithej.core.Validate;

/**
 * The outcome of a completed HTTP exchange performed by {@link Http}: status code,
 * response headers, and the fully-buffered response body.
 *
 * <p>Receiving a response at all &mdash; regardless of its status code &mdash; is a
 * normal, successful outcome from {@code Http}'s point of view; a {@code 404} or
 * {@code 500} response is returned as an ordinary {@code HttpResponse} with
 * {@link #isSuccessful()} reporting {@code false}, not thrown as an exception. See
 * {@link HttpRequestException} for the failures that are thrown instead (connection
 * failure, timeout, interruption).
 *
 * <p>{@link #body()} decodes {@link #bodyBytes()} as UTF-8 unconditionally; it does
 * not inspect the {@code Content-Type} response header for a different charset. If
 * the server responds with bytes encoded in another charset, use
 * {@link #bodyBytes()} and decode them yourself.
 *
 * <p>For anything not exposed here, {@link #raw()} returns the underlying
 * {@link java.net.http.HttpResponse} produced by {@link java.net.http.HttpClient}.
 *
 * <p><b>Sensitive headers:</b> {@link #toString()} redacts the value of any header
 * whose name looks like it carries a secret (see {@link SensitiveHeaders}), printing
 * {@code ***} instead of the real value. {@link #headers()} is unaffected and always
 * returns the real values.
 *
 * <p><b>Thread safety:</b> immutable and thread-safe. {@link #bodyBytes()} returns a
 * defensive copy on every call so callers cannot observe or mutate this instance's
 * internal state.
 */
public final class HttpResponse {

    private static final int SUCCESS_STATUS_MIN = 200;
    private static final int SUCCESS_STATUS_MAX = 300;

    private final int statusCode;
    private final Map<String, List<String>> headers;
    private final byte[] bodyBytes;
    private final java.net.http.HttpResponse<byte[]> raw;

    HttpResponse(java.net.http.HttpResponse<byte[]> raw) {
        this.raw = Validate.notNull(raw, "raw");
        this.statusCode = raw.statusCode();
        this.headers = Map.copyOf(raw.headers().map());
        byte[] body = raw.body();
        this.bodyBytes = body == null ? new byte[0] : body.clone();
    }

    /**
     * Returns the HTTP status code of the response, e.g. {@code 200} or {@code 404}.
     *
     * @return the HTTP status code
     */
    public int statusCode() {
        return statusCode;
    }

    /**
     * Returns the response headers.
     *
     * @return an unmodifiable map of header names to their values, in the order the
     *     server sent them; never {@code null}
     */
    public Map<String, List<String>> headers() {
        return headers;
    }

    /**
     * Returns the response body as raw bytes.
     *
     * @return a defensive copy of the response body; never {@code null}, possibly
     *     empty
     */
    public byte[] bodyBytes() {
        return bodyBytes.clone();
    }

    /**
     * Returns the response body decoded as UTF-8.
     *
     * @return the response body decoded as UTF-8; never {@code null}, possibly empty
     */
    public String body() {
        return new String(bodyBytes, StandardCharsets.UTF_8);
    }

    /**
     * Returns {@code true} if {@link #statusCode()} is in the {@code [200, 300)}
     * range, i.e. a conventional "successful" HTTP status.
     *
     * @return {@code true} if the status code indicates success
     */
    public boolean isSuccessful() {
        return statusCode >= SUCCESS_STATUS_MIN && statusCode < SUCCESS_STATUS_MAX;
    }

    /**
     * Returns the underlying JDK response object, for access to anything this class
     * does not wrap (e.g. {@link java.net.http.HttpResponse#version()},
     * {@link java.net.http.HttpResponse#sslSession()}, or the previous responses in a
     * redirect chain).
     *
     * @return the underlying {@link java.net.http.HttpResponse}
     */
    public java.net.http.HttpResponse<byte[]> raw() {
        return raw;
    }

    @Override
    public String toString() {
        Map<String, List<String>> displayHeaders = new LinkedHashMap<>();
        headers.forEach((name, values) -> displayHeaders.put(name,
                SensitiveHeaders.isSensitive(name) ? List.of(SensitiveHeaders.REDACTED) : values));
        return "HttpResponse[statusCode=" + statusCode
                + ", headers=" + displayHeaders
                + ", bodyBytes=" + bodyBytes.length + " bytes]";
    }
}
