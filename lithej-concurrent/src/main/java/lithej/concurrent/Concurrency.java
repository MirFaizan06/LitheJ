package lithej.concurrent;

/**
 * Entry point for structured concurrency: {@link #scope()} creates a
 * {@link TaskScope} that forks work onto virtual threads and guarantees none of it
 * can outlive the scope. See {@link TaskScope} for the full guarantee and usage.
 *
 * <p><b>Thread safety:</b> this class is stateless and thread-safe.
 */
public final class Concurrency {

    private Concurrency() {
    }

    /**
     * Creates a scope with the default {@link FailurePolicy#FAIL_FAST} policy: the
     * first task failure cancels every other task still running in the scope.
     *
     * @return a new scope; always use it inside try-with-resources
     */
    public static TaskScope scope() {
        return new TaskScope(FailurePolicy.FAIL_FAST);
    }

    /**
     * Creates a scope with an explicit failure policy.
     *
     * @param policy how the scope should react to a task failure
     * @return a new scope; always use it inside try-with-resources
     * @throws NullPointerException if {@code policy} is {@code null}
     */
    public static TaskScope scope(FailurePolicy policy) {
        return new TaskScope(policy);
    }
}
