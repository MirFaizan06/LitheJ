package lithej.concurrent;

/**
 * Controls how a {@link TaskScope} reacts when one of its forked tasks fails.
 */
public enum FailurePolicy {

    /**
     * The default. The first task failure immediately interrupts every other
     * still-running task in the scope, and {@link TaskScope#joinAll()} throws
     * {@link TaskFailedException} wrapping that first failure's cause as soon as every
     * task has actually finished (interruption is cooperative, so "immediately" means
     * "as soon as the running tasks notice the interrupt").
     */
    FAIL_FAST,

    /**
     * No task is cancelled because another one failed; {@link TaskScope#joinAll()}
     * waits for every task to finish regardless of individual outcomes, then throws
     * {@link MultipleTaskFailuresException} listing every failure (in the order the
     * tasks were forked) if at least one task failed.
     */
    COLLECT_ALL
}
