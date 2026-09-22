package lithej.concurrent;

import java.io.Serial;
import java.util.List;

/**
 * Thrown by {@link TaskScope#joinAll()} under {@link FailurePolicy#COLLECT_ALL} when
 * one or more forked tasks fail. Every failure is preserved: the
 * {@linkplain #getCause() cause} is the first failure (in fork order), and
 * {@link #failures()} lists every failure, including that first one.
 *
 * @see TaskFailedException
 */
public final class MultipleTaskFailuresException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    private final transient List<Throwable> failures;

    /**
     * Creates an exception wrapping every task failure from a scope.
     *
     * @param message a description of the failure
     * @param failures every task failure, in fork order; must not be empty
     * @throws IllegalArgumentException if {@code failures} is empty
     */
    public MultipleTaskFailuresException(String message, List<Throwable> failures) {
        super(message, failures.isEmpty() ? null : failures.get(0));
        if (failures.isEmpty()) {
            throw new IllegalArgumentException("failures must not be empty");
        }
        this.failures = List.copyOf(failures);
    }

    /**
     * Returns every task failure that caused this exception, in fork order.
     *
     * @return an unmodifiable list of every failure; never empty
     */
    public List<Throwable> failures() {
        return failures;
    }
}
