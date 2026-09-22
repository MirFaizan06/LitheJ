package lithej.concurrent;

import java.io.Serial;

/**
 * Thrown by {@link TaskScope#joinAll()} (under {@link FailurePolicy#FAIL_FAST}, the
 * default) when a forked task fails. The {@linkplain #getCause() cause} is the
 * failing task's own exception, unwrapped.
 *
 * @see MultipleTaskFailuresException
 */
public final class TaskFailedException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception wrapping a task's failure.
     *
     * @param message a description of the failure
     * @param cause the failing task's exception
     */
    public TaskFailedException(String message, Throwable cause) {
        super(message, cause);
    }
}
