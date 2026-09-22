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
     * Creates an exception describing the timeout that elapsed.
     *
     * @param timeout the configured timeout that elapsed
     */
    public AsyncTimeoutException(Duration timeout) {
        super("Asynchronous operation did not complete within " + timeout);
    }
}
