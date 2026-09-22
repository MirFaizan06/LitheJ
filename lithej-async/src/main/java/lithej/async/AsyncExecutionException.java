package lithej.async;

import java.io.Serial;

/**
 * Thrown by {@link Async#await} when the awaited {@link java.util.concurrent.CompletableFuture}
 * failed with a checked exception, or when the calling thread was interrupted while
 * waiting for it.
 *
 * <p>An unchecked failure of the asynchronous computation itself (a
 * {@link RuntimeException} or {@link Error} thrown by the task) is <b>not</b> wrapped:
 * {@link Async#await} rethrows it directly instead, so this exception's
 * {@linkplain #getCause() cause} is always either a checked {@link Exception} or an
 * {@link InterruptedException}.
 *
 * <p>When this exception wraps an {@link InterruptedException}, the calling thread's
 * interrupt status has already been restored (via {@link Thread#interrupt()}) by the
 * code that threw it, per standard Java practice for handling interruption.
 */
public final class AsyncExecutionException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception wrapping {@code cause}.
     *
     * @param cause the checked exception or {@link InterruptedException} that caused
     *     the asynchronous operation to fail
     */
    public AsyncExecutionException(Throwable cause) {
        super("Asynchronous operation failed: " + cause, cause);
    }
}
