package lithej.process;

import java.io.Serial;

/**
 * Thrown by {@link ProcessX} when a process cannot be started, or when the calling
 * thread is interrupted while waiting for it to exit.
 *
 * @see ProcessTimeoutException
 */
public class ProcessExecutionException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception with the given message and cause.
     *
     * @param message a description of what went wrong
     * @param cause the underlying exception (typically an {@link java.io.IOException}
     *     or an {@link InterruptedException})
     */
    public ProcessExecutionException(String message, Throwable cause) {
        super(message, cause);
    }
}
