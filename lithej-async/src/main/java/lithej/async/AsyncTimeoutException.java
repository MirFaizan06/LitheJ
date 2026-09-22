package lithej.async;

import java.io.Serial;
import java.time.Duration;

/**
 * Thrown when an asynchronous operation does not complete within its configured
 * timeout: by {@link Async#await} for the blocking, wait-and-throw case, and by
 * {@link Async#withTimeout} for the non-blocking, future-fails-exceptionally case.
 */
public final class AsyncTimeoutException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception describing the timeout that elapsed, with no known cause.
     *
     * @param timeout the configured timeout that elapsed
     */
    public AsyncTimeoutException(Duration timeout) {
        this(timeout, null);
    }

    /**
     * Creates an exception describing the timeout that elapsed, preserving the
     * underlying {@link java.util.concurrent.TimeoutException} (or other throwable) that
     * triggered it, if any, as this exception's cause.
     *
     * @param timeout the configured timeout that elapsed
     * @param cause the underlying exception that signaled the timeout, or {@code null}
     *     if none is available
     */
    public AsyncTimeoutException(Duration timeout, Throwable cause) {
        super("Asynchronous operation did not complete within " + timeout, cause);
    }
}
