package lithej.concurrent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.atomic.AtomicReference;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordedStackTrace;
import jdk.jfr.consumer.RecordedThread;
import jdk.jfr.consumer.RecordingStream;
import lithej.core.Validate;

/**
 * A scope that forks tasks onto virtual threads and guarantees, structurally, that
 * none of them can outlive the scope.
 *
 * <p>Create one with {@link Concurrency#scope()}, always inside try-with-resources:
 *
 * <pre>{@code
 * try (TaskScope scope = Concurrency.scope()) {
 *     Subtask<User> user = scope.fork(() -> fetchUser(id));
 *     Subtask<List<Order>> orders = scope.fork(() -> fetchOrders(id));
 *
 *     scope.joinAll(); // waits for both; cancels the other on first failure
 *
 *     return new Profile(user.get(), orders.get());
 * }
 * }</pre>
 *
 * <h2>The guarantee</h2>
 *
 * <p>{@link #close()} always waits for every forked task to actually finish before
 * returning, whether or not {@link #joinAll()} was ever called and whether the
 * enclosing try block exits normally, via a thrown exception, or by never calling
 * {@code joinAll()} at all. A forked task can never keep running after its scope's
 * try-with-resources block has exited. There is no equivalent guarantee from a
 * hand-managed {@link java.util.concurrent.ExecutorService} plus a list of
 * {@link java.util.concurrent.Future}s: forgetting one {@code future.get()} silently
 * leaks a thread. Forgetting {@link #joinAll()} here does not.
 *
 * <p>{@link #close()} also never silently discards a task failure: if
 * {@code joinAll()} was never called, {@code close()} performs the same failure check
 * itself and throws. If the try block already threw some other exception,
 * {@code close()}'s own exception (if any) is attached to it as a
 * {@linkplain Throwable#addSuppressed(Throwable) suppressed exception} rather than
 * replacing it, per normal try-with-resources semantics.
 *
 * <h2>Pinning diagnostics</h2>
 *
 * <p>While a scope has at least one forked task, it records
 * {@code jdk.VirtualThreadPinned} JFR events. After the scope closes,
 * {@link #pinningEvents()} lists every pinning event observed during its lifetime.
 * This is zero-configuration: no JVM flags, no separate profiling run. The recording
 * itself has a small cost (a background JFR stream for the scope's lifetime); for
 * very short, very high-frequency scopes in a hot loop, prefer batching work into
 * fewer, longer-lived scopes. See {@link PinningEvent} for what counts as pinning and
 * how that changed in JDK 24.
 *
 * <p><b>Thread safety:</b> {@link #fork(Callable)} may be called concurrently from
 * multiple threads. {@link #joinAll()} and {@link #close()} are intended to be called
 * once each, from the thread that created the scope, after all forking is done;
 * calling either concurrently with an in-progress {@code fork()} has undefined
 * ordering with respect to that fork (the forked task may or may not be waited for).
 */
public final class TaskScope implements AutoCloseable {

    private static final String EVENT_NAME = "jdk.VirtualThreadPinned";
    private static final int STACK_SUMMARY_MAX_FRAMES = 8;

    private final FailurePolicy policy;
    private final List<ForkedTask<?>> forkedTasks = new CopyOnWriteArrayList<>();
    private final List<PinningEvent> pinningEvents = new CopyOnWriteArrayList<>();
    private final AtomicBoolean cancelled = new AtomicBoolean(false);
    private final AtomicBoolean resultConsumed = new AtomicBoolean(false);
    private final AtomicLong nextThreadOrdinal = new AtomicLong(0);
    private final AtomicReference<RecordingStream> recordingStreamRef = new AtomicReference<>();

    TaskScope(FailurePolicy policy) {
        this.policy = Validate.notNull(policy, "policy");
    }

    /**
     * Forks {@code task} onto a new virtual thread owned by this scope.
     *
     * @param task the work to run
     * @param <T> the task's result type
     * @return a handle to observe the task's outcome once the scope has joined
     * @throws NullPointerException if {@code task} is {@code null}
     */
    public <T> Subtask<T> fork(Callable<T> task) {
        Validate.notNull(task, "task");
        ensurePinningRecordingStarted();

        Subtask<T> subtask = new Subtask<>();
        // Named (not left as the JDK's default unnamed ""), so a PinningEvent's
        // threadName() and any thread dump are actually useful for telling which
        // forked task is which, instead of every entry looking identical.
        Thread thread = Thread.ofVirtual()
                .name("lithej-concurrent-", nextThreadOrdinal.getAndIncrement())
                .unstarted(() -> runTask(task, subtask));
        forkedTasks.add(new ForkedTask<>(thread, subtask));
        thread.start();
        return subtask;
    }

    private <T> void runTask(Callable<T> task, Subtask<T> subtask) {
        try {
            subtask.succeed(task.call());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            subtask.cancel();
        } catch (Exception e) {
            subtask.fail(e);
            if (policy == FailurePolicy.FAIL_FAST) {
                triggerCancellation();
            }
        } catch (Error e) {
            subtask.fail(e);
            triggerCancellation();
            throw e;
        }
    }

    private void triggerCancellation() {
        if (cancelled.compareAndSet(false, true)) {
            for (ForkedTask<?> forked : forkedTasks) {
                if (forked.subtask.state() == Subtask.State.RUNNING) {
                    forked.thread.interrupt();
                }
            }
        }
    }

    /**
     * Waits for every forked task to finish, then throws if any of them failed.
     *
     * <p>Under {@link FailurePolicy#FAIL_FAST} (the default), throws
     * {@link TaskFailedException} wrapping the first failure (in fork order) if any
     * task failed; sibling tasks still running at that point were already
     * interrupted. Under {@link FailurePolicy#COLLECT_ALL}, throws
     * {@link MultipleTaskFailuresException} listing every failure if at least one task
     * failed.
     *
     * @throws TaskFailedException if a task failed under {@link FailurePolicy#FAIL_FAST}
     * @throws MultipleTaskFailuresException if one or more tasks failed under
     *     {@link FailurePolicy#COLLECT_ALL}
     */
    public void joinAll() {
        joinAllThreads();
        checkAndThrow();
    }

    /**
     * Waits for every forked task to finish (guaranteeing none can outlive this
     * scope), and, if {@link #joinAll()} was never called, performs its failure check
     * as well.
     *
     * @throws TaskFailedException if {@link #joinAll()} was never called and a task
     *     failed under {@link FailurePolicy#FAIL_FAST}
     * @throws MultipleTaskFailuresException if {@link #joinAll()} was never called and
     *     one or more tasks failed under {@link FailurePolicy#COLLECT_ALL}
     */
    @Override
    public void close() {
        joinAllThreads();
        try {
            checkAndThrow();
        } finally {
            stopPinningRecording();
        }
    }

    private void joinAllThreads() {
        boolean interrupted = false;
        for (ForkedTask<?> forked : forkedTasks) {
            while (true) {
                try {
                    forked.thread.join();
                    break;
                } catch (InterruptedException e) {
                    interrupted = true;
                    triggerCancellation();
                }
            }
        }
        if (interrupted) {
            Thread.currentThread().interrupt();
        }
    }

    private void checkAndThrow() {
        if (!resultConsumed.compareAndSet(false, true)) {
            return;
        }
        List<Throwable> failures = new ArrayList<>();
        for (ForkedTask<?> forked : forkedTasks) {
            if (forked.subtask.state() == Subtask.State.FAILED) {
                failures.add(forked.subtask.exception());
            }
        }
        if (failures.isEmpty()) {
            return;
        }
        if (policy == FailurePolicy.FAIL_FAST) {
            throw new TaskFailedException("A forked task failed", failures.get(0));
        }
        throw new MultipleTaskFailuresException(failures.size() + " forked task(s) failed", failures);
    }

    /**
     * Returns every virtual-thread pinning event observed while this scope had at
     * least one forked task running.
     *
     * <p><b>Only guaranteed complete after {@link #close()} has returned.</b> JFR
     * delivers events to this scope's recording asynchronously; {@code close()} (via
     * {@code RecordingStream.stop()}) is what flushes any events still in flight for
     * work that already finished. Calling this immediately after {@link #joinAll()}
     * but before {@code close()} may still miss very recent events — join all your
     * tasks, close the scope, <em>then</em> read this.
     *
     * @return an unmodifiable, possibly-empty list of pinning events, in the order
     *     they were observed
     */
    public List<PinningEvent> pinningEvents() {
        return List.copyOf(pinningEvents);
    }

    // The RecordingStream created here is stored in recordingStreamRef and closed
    // later by stopPinningRecording() (from close()) -- a resource whose lifetime is
    // tied to this object rather than one local scope, which PMD's CloseResource
    // check cannot trace across methods/fields. It is genuinely closed; see
    // stopPinningRecording() and the tests in PinningDiagnosticsTest.
    @SuppressWarnings("PMD.CloseResource")
    private void ensurePinningRecordingStarted() {
        if (recordingStreamRef.get() != null) {
            return;
        }
        RecordingStream candidate = new RecordingStream();
        candidate.enable(EVENT_NAME).withStackTrace();
        candidate.onEvent(EVENT_NAME, this::recordPinningEvent);
        if (recordingStreamRef.compareAndSet(null, candidate)) {
            candidate.startAsync();
        } else {
            // Lost a race with a concurrent fork() call; the winner's stream is
            // already active, so this one was never started and just needs closing.
            candidate.close();
        }
    }

    private void recordPinningEvent(RecordedEvent event) {
        RecordedThread thread = event.getThread();
        if (thread == null || !isOwnThread(thread.getId())) {
            // Not one of this scope's own forked threads: JFR recording streams are
            // JVM-wide, so without this check a pinning event from unrelated code
            // running concurrently elsewhere in the process would be misattributed
            // to this scope.
            return;
        }
        pinningEvents.add(new PinningEvent(thread.getJavaName(), event.getDuration(), summarizeStackTrace(event)));
    }

    private boolean isOwnThread(long threadId) {
        for (ForkedTask<?> forked : forkedTasks) {
            if (forked.thread.threadId() == threadId) {
                return true;
            }
        }
        return false;
    }

    private static String summarizeStackTrace(RecordedEvent event) {
        if (!event.hasField("stackTrace")) {
            return "";
        }
        RecordedStackTrace stackTrace = event.getStackTrace();
        if (stackTrace == null) {
            return "";
        }
        StringBuilder summary = new StringBuilder();
        int frameCount = Math.min(STACK_SUMMARY_MAX_FRAMES, stackTrace.getFrames().size());
        for (int i = 0; i < frameCount; i++) {
            if (i > 0) {
                summary.append('\n');
            }
            summary.append(stackTrace.getFrames().get(i).getMethod().getType().getName())
                    .append('.')
                    .append(stackTrace.getFrames().get(i).getMethod().getName());
        }
        return summary.toString();
    }

    // Cannot use try-with-resources here: the interrupt-status save/restore must
    // wrap both stop() and close() as a single unit (see the comment below for why),
    // which a plain try-with-resources has no hook for. PMD's CloseResource check
    // also does not see this as a "close" because the resource came from a field via
    // a getter, not a local variable created in this method -- it genuinely is
    // closed on every path (see the finally block).
    @SuppressWarnings({"PMD.UseTryWithResources", "PMD.CloseResource"})
    private void stopPinningRecording() {
        RecordingStream stream = recordingStreamRef.get();
        if (stream == null) {
            return;
        }
        // RecordingStream#stop()/close() have been observed to silently clear the
        // calling thread's interrupt status as an internal side effect (presumably
        // from their own use of an interruptible wait while flushing/joining JFR's
        // delivery thread). That would break this scope's promise that a genuine
        // interrupt on the joining thread survives close() -- capture and restore it
        // unconditionally around this call so JFR's internals cannot swallow it.
        boolean wasInterrupted = Thread.interrupted();
        try {
            stream.stop();
        } finally {
            stream.close();
            if (wasInterrupted) {
                Thread.currentThread().interrupt();
            }
        }
    }

    private record ForkedTask<T>(Thread thread, Subtask<T> subtask) {
    }
}
