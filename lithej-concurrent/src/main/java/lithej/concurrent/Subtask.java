package lithej.concurrent;

import java.util.concurrent.atomic.AtomicReference;

/**
 * A handle to one task forked into a {@link TaskScope}, obtained from
 * {@link TaskScope#fork(java.util.concurrent.Callable)}.
 *
 * <p>A {@code Subtask} starts {@link State#RUNNING} and moves to exactly one final
 * state once its task completes: {@link State#SUCCESS}, {@link State#FAILED}, or
 * {@link State#CANCELLED} (interrupted as a result of a sibling task's failure under
 * {@link FailurePolicy#FAIL_FAST}). The final state, and {@link #get()}/
 * {@link #exception()}, are only meaningful after the owning scope's
 * {@link TaskScope#joinAll()} or {@link TaskScope#close()} has returned.
 *
 * <p>Instances are safe to read from any thread once the owning scope has joined;
 * there is no supported way to observe a partially-updated state.
 *
 * @param <T> the task's result type
 */
public final class Subtask<T> {

    /**
     * The lifecycle states of a {@link Subtask}.
     */
    public enum State {
        /** The task has not yet completed. */
        RUNNING,
        /** The task returned a value. */
        SUCCESS,
        /** The task threw an exception. */
        FAILED,
        /**
         * The task was interrupted as a result of a sibling task's failure under
         * {@link FailurePolicy#FAIL_FAST}, and propagated that interruption as
         * {@link InterruptedException} in the normal way. A cancelled task is not
         * counted as a failure by {@link TaskScope#joinAll()}.
         */
        CANCELLED
    }

    // volatile is required here, not just a style choice PMD's AvoidUsingVolatile
    // objects to on principle: TaskScope.triggerCancellation() reads state() from the
    // joining thread to decide whether to interrupt a sibling while that sibling's
    // own task thread may still be concurrently writing it via succeed()/fail()/
    // cancel() -- there is no join() between those two threads at that point for a
    // happens-before edge to come from otherwise.
    @SuppressWarnings("PMD.AvoidUsingVolatile")
    private volatile State state = State.RUNNING;
    // Same rationale as state() above: get() may be called from a different thread
    // than the one that wrote the value via succeed(), without an intervening join().
    @SuppressWarnings("PMD.AvoidUsingVolatile")
    private volatile T value;
    private final AtomicReference<Throwable> failure = new AtomicReference<>();

    Subtask() {
    }

    void succeed(T result) {
        this.value = result;
        this.state = State.SUCCESS;
    }

    void fail(Throwable cause) {
        this.failure.set(cause);
        this.state = State.FAILED;
    }

    void cancel() {
        this.state = State.CANCELLED;
    }

    /**
     * Returns this task's current state.
     *
     * @return the current state
     */
    public State state() {
        return state;
    }

    /**
     * Returns the value this task completed with.
     *
     * @return the task's result
     * @throws IllegalStateException if {@link #state()} is not {@link State#SUCCESS}
     */
    public T get() {
        if (state != State.SUCCESS) {
            throw new IllegalStateException("Subtask did not complete successfully (state=" + state + ")");
        }
        return value;
    }

    /**
     * Returns the exception this task failed with.
     *
     * @return the task's failure
     * @throws IllegalStateException if {@link #state()} is not {@link State#FAILED}
     */
    public Throwable exception() {
        if (state != State.FAILED) {
            throw new IllegalStateException("Subtask did not fail (state=" + state + ")");
        }
        return failure.get();
    }

    @Override
    public String toString() {
        return "Subtask[state=" + state + "]";
    }
}
