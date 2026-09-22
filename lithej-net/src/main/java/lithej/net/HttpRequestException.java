package lithej.net;

import java.io.Serial;

/**
 * Thrown by {@link Http} when an HTTP exchange cannot be completed at all: the
 * connection could not be established (e.g. connection refused, DNS failure,
 * redirect loop), the request timed out, or the calling thread was interrupted while
 * waiting for the response.
 *
 * <p>A response with a non-2xx status code (e.g. {@code 404 Not Found} or
 * {@code 500 Internal Server Error}) is <b>not</b> represented by this exception: from
 * {@code Http}'s point of view, receiving any HTTP status code, headers, and body is a
 * normal, successfully-completed exchange, returned as an ordinary
 * {@link HttpResponse} with {@link HttpResponse#isSuccessful()} reporting
 * {@code false}. This exception is reserved for exchanges that could not happen at
 * all.
 *
 * <p>When this exception wraps an {@link InterruptedException}, the thread's interrupt
 * status has already been restored (via {@link Thread#interrupt()}) by the code that
 * threw it, per standard Java practice for handling interruption.
 */
public final class HttpRequestException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with the given message and cause.
     *
     * @param message a description of what went wrong
     * @param cause the underlying exception (typically an {@link java.io.IOException}
     *     or an {@link InterruptedException})
     */
    public HttpRequestException(String message, Throwable cause) {
        super(message, cause);
    }
}
