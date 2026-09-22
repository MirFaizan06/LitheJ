package lithej.async;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Supplier;
import java.util.stream.Collectors;
import lithej.core.Validate;

/**
 * Ergonomic helpers around {@link CompletableFuture}: running tasks, waiting for one of
 * several futures, waiting for all of them, and applying a timeout.
 *
 * <p>This class does not implement its own concurrency: every method is a thin,
 * well-documented layer over the standard JDK {@link CompletableFuture} and
 * {@link Executor} APIs. Nothing here creates a thread pool. Methods that schedule
 * asynchronous work take an explicit {@link Executor} parameter, with the single
 * documented exception of {@link #run(Supplier)}, which uses
 * {@link ForkJoinPool#commonPool()} exactly as bare {@code CompletableFuture.supplyAsync}
 * does.
 *
 * <p><b>Thread safety:</b> this class is stateless and thread-safe. The futures it
 * returns are ordinary {@link CompletableFuture} instances and inherit that class's own
 * thread-safety guarantees.
 */
public final class Async {

    private static final String PARAM_TASK = "task";
    private static final String PARAM_EXECUTOR = "executor";
    private static final String PARAM_FUTURE = "future";
    private static final String PARAM_FUTURES = "futures";
    private static final String PARAM_TIMEOUT = "timeout";

    private Async() {
    }

    /**
     * Runs {@code task} asynchronously and returns a future for its result.
     *
     * <p><b>This method runs {@code task} on the JDK's shared {@link
     * ForkJoinPool#commonPool()}</b> — it is exactly equivalent to calling {@code
     * CompletableFuture.supplyAsync(task)} directly. The common pool is shared across the
     * entire JVM, sized to the number of available processors, and can be starved or
     * saturated by unrelated code (e.g. parallel streams) running elsewhere in the same
     * process. For CPU-bound work that should not compete with other common-pool users,
     * or for I/O-bound work that may block a common-pool thread for a long time, use
     * {@link #run(Supplier, Executor)} with an executor sized and dedicated for that
     * purpose instead.
     *
     * <pre>{@code
     * CompletableFuture<Integer> answer = Async.run(() -> 6 * 7);
     * }</pre>
     *
     * @param task the computation to run
     * @param <T> the result type
     * @return a future completed with {@code task}'s result, or completed exceptionally
     *     if {@code task} throws
     * @throws NullPointerException if {@code task} is {@code null}
     */
    public static <T> CompletableFuture<T> run(Supplier<T> task) {
        Validate.notNull(task, PARAM_TASK);
        return CompletableFuture.supplyAsync(task);
    }

    /**
     * Runs {@code task} asynchronously on {@code executor} and returns a future for its
     * result.
     *
     * <p>This is the recommended form of {@code run}: the caller decides exactly which
     * {@link Executor} the work runs on, so it is easy to size and monitor a dedicated
     * pool, and impossible to accidentally starve or be starved by unrelated work sharing
     * the JDK's common pool.
     *
     * <pre>{@code
     * ExecutorService pool = Executors.newFixedThreadPool(4);
     * CompletableFuture<String> result = Async.run(() -> loadReport(), pool);
     * }</pre>
     *
     * @param task the computation to run
     * @param executor the executor to run {@code task} on
     * @param <T> the result type
     * @return a future completed with {@code task}'s result, or completed exceptionally
     *     if {@code task} throws
     * @throws NullPointerException if {@code task} or {@code executor} is {@code null}
     */
    public static <T> CompletableFuture<T> run(Supplier<T> task, Executor executor) {
        Validate.notNull(task, PARAM_TASK);
        Validate.notNull(executor, PARAM_EXECUTOR);
        return CompletableFuture.supplyAsync(task, executor);
    }

    /**
     * Runs {@code task} asynchronously on {@code executor} for its side effects only.
     *
     * <p>There is deliberately no overload of this method with a default executor: for
     * fire-and-forget work where nothing observes the returned future's result, an
     * unbounded or unannounced default executor is more likely to silently accumulate
     * unmanaged work than in the value-returning case. Callers must always say where this
     * runs.
     *
     * <p>The returned future still completes exceptionally if {@code task} throws;
     * callers who care about failures should attach a handler (e.g. {@code
     * exceptionally}, {@code whenComplete}) rather than discarding the returned future.
     *
     * <pre>{@code
     * ExecutorService pool = Executors.newFixedThreadPool(2);
     * Async.runAsync(() -> auditLog.write(event), pool)
     *         .exceptionally(ex -> { logger.warn("audit write failed", ex); return null; });
     * }</pre>
     *
     * @param task the action to run
     * @param executor the executor to run {@code task} on
     * @return a future completed with {@code null} once {@code task} finishes, or
     *     completed exceptionally if {@code task} throws
     * @throws NullPointerException if {@code task} or {@code executor} is {@code null}
     */
    public static CompletableFuture<Void> runAsync(Runnable task, Executor executor) {
        Validate.notNull(task, PARAM_TASK);
        Validate.notNull(executor, PARAM_EXECUTOR);
        return CompletableFuture.runAsync(task, executor);
    }

    /**
     * Waits for every future in {@code futures} to complete and collects their results in
     * the same order as the input list.
     *
     * <p>If {@code futures} is empty, the returned future is already completed with an
     * empty list. Otherwise, the returned future completes only once every input future
     * has completed (successfully or not).
     *
     * <p>If any input future fails, the returned future fails too, with the real cause of
     * the first failure it observes (unwrapped from the {@link CompletionException} that
     * {@link CompletableFuture} itself uses internally to propagate failures between
     * stages) as its own completion exception. Concretely: calling {@code get()} on the
     * returned future throws {@link ExecutionException} whose {@linkplain
     * Throwable#getCause() cause} is exactly that original exception, and calling {@code
     * join()} throws a {@link CompletionException} wrapping it — the same behavior you
     * would see calling {@code get()}/{@code join()} on the failed input future directly.
     * This method does not attempt to collect or report more than one failure.
     *
     * <pre>{@code
     * List<CompletableFuture<String>> lookups = List.of(
     *         Async.run(() -> fetch("a"), pool),
     *         Async.run(() -> fetch("b"), pool));
     * List<String> results = Async.all(lookups).join(); // in input order
     * }</pre>
     *
     * @param futures the futures to wait for; the list itself is not modified or
     *     retained, but is defensively copied
     * @param <T> the result type common to every future
     * @return a future completed with every result in input order once all of {@code
     *     futures} have completed successfully, or completed exceptionally with the first
     *     observed failure's cause
     * @throws NullPointerException if {@code futures}, or any element of it, is {@code
     *     null}
     */
    public static <T> CompletableFuture<List<T>> all(List<CompletableFuture<T>> futures) {
        List<CompletableFuture<T>> copy = copyRejectingNullElements(futures, PARAM_FUTURES);
        if (copy.isEmpty()) {
            return CompletableFuture.completedFuture(List.of());
        }
        CompletableFuture<List<T>> result = new CompletableFuture<>();
        CompletableFuture.allOf(copy.toArray(new CompletableFuture<?>[0]))
                .whenComplete((ignored, throwable) -> {
                    if (throwable != null) {
                        result.completeExceptionally(unwrapCompletionException(throwable));
                    } else {
                        result.complete(copy.stream().map(CompletableFuture::join).collect(Collectors.toUnmodifiableList()));
                    }
                });
        return result;
    }

    /**
     * Returns a future that completes as soon as the first future in {@code futures}
     * completes, whether that future succeeds or fails.
     *
     * <p><b>A failing future can "win" the race just as a succeeding one can.</b> This
     * method mirrors the real semantics of {@link CompletableFuture#anyOf}: if the first
     * input future to complete does so exceptionally, the returned future completes
     * exceptionally too, with that same failure, even if other input futures later go on
     * to succeed. Callers that only want the first <em>success</em> must filter or retry
     * themselves; this method does not do that on their behalf.
     *
     * <pre>{@code
     * CompletableFuture<String> fastest = Async.anyOf(List.of(mirrorA, mirrorB, mirrorC));
     * }</pre>
     *
     * @param futures the futures to race; must not be empty
     * @param <T> the result type common to every future
     * @return a future completed with the outcome (success or failure) of whichever
     *     input future completes first
     * @throws NullPointerException if {@code futures}, or any element of it, is {@code
     *     null}
     * @throws IllegalArgumentException if {@code futures} is empty (racing zero futures
     *     would never complete, per {@link CompletableFuture#anyOf})
     */
    public static <T> CompletableFuture<T> anyOf(List<CompletableFuture<T>> futures) {
        List<CompletableFuture<T>> copy = copyRejectingNullElements(futures, PARAM_FUTURES);
        Validate.notEmpty(copy, PARAM_FUTURES);
        CompletableFuture<Object> raced = CompletableFuture.anyOf(copy.toArray(new CompletableFuture<?>[0]));
        return raced.thenApply(Async::castResult);
    }

    /**
     * Blocks the calling thread until {@code future} completes or {@code timeout}
     * elapses, returning its result.
     *
     * <p>This is a checked-exception-free alternative to calling {@code future.get(long,
     * TimeUnit)} directly:
     *
     * <ul>
     *   <li>if {@code future} failed with an unchecked exception ({@link RuntimeException}
     *       or {@link Error}), that exception is rethrown as-is;
     *   <li>if {@code future} failed with a checked exception, it is wrapped in a new
     *       {@link AsyncExecutionException};
     *   <li>if {@code timeout} elapses first, an {@link AsyncTimeoutException} is thrown;
     *   <li>if the calling thread is interrupted while waiting, its interrupt status is
     *       restored (via {@link Thread#interrupt()}) and an {@link
     *       AsyncExecutionException} wrapping the {@link InterruptedException} is thrown.
     * </ul>
     *
     * <pre>{@code
     * String value = Async.await(future, Duration.ofSeconds(5));
     * }</pre>
     *
     * @param future the future to wait for
     * @param timeout the maximum time to wait
     * @param <T> the result type
     * @return {@code future}'s result
     * @throws NullPointerException if {@code future} or {@code timeout} is {@code null}
     * @throws AsyncTimeoutException if {@code timeout} elapses before {@code future}
     *     completes
     * @throws AsyncExecutionException if {@code future} failed with a checked exception,
     *     or if the calling thread was interrupted while waiting
     */
    public static <T> T await(CompletableFuture<T> future, Duration timeout) {
        Validate.notNull(future, PARAM_FUTURE);
        Validate.notNull(timeout, PARAM_TIMEOUT);
        try {
            return future.get(timeout.toNanos(), TimeUnit.NANOSECONDS);
        } catch (ExecutionException e) {
            // Deliberately unwrap and rethrow the task's own real failure (preserving
            // ITS stack trace, which points at the actual failure) rather than wrapping
            // the uninteresting ExecutionException that CompletableFuture#get adds -
            // this is the documented contract of this method, not an oversight.
            Throwable cause = e.getCause();
            if (cause instanceof RuntimeException runtimeException) {
                throw runtimeException; // NOPMD - see comment above
            }
            if (cause instanceof Error error) {
                throw error; // NOPMD - see comment above
            }
            throw new AsyncExecutionException(cause != null ? cause : e); // NOPMD - cause is e itself when null
        } catch (TimeoutException e) {
            throw new AsyncTimeoutException(timeout, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AsyncExecutionException(e);
        }
    }

    /**
     * Returns a new future that adopts {@code future}'s outcome if it completes within
     * {@code timeout}, or fails with an {@link AsyncTimeoutException} otherwise — without
     * blocking any thread to wait for it.
     *
     * <p>This is built on {@link CompletableFuture#orTimeout(long, TimeUnit)}, and
     * inherits an important, easy-to-miss detail of that method that is worth stating
     * plainly: <b>{@code orTimeout} schedules the timeout on {@code future} itself and
     * returns that same instance — it does not operate on a private copy.</b> If {@code
     * future} does not complete in time, {@code future} itself becomes exceptionally
     * completed with a {@link TimeoutException}. Any other code that already holds a
     * reference to {@code future} — not just the future returned by this method — will
     * observe that same exceptional completion. The future returned by this method wraps
     * that outcome and reports the timeout as an {@link AsyncTimeoutException} instead
     * (so callers of {@code withTimeout} see the same exception type {@link #await}
     * throws), but the underlying mutation of {@code future} is shared, real, and visible
     * to anyone else watching it.
     *
     * <pre>{@code
     * CompletableFuture<String> bounded = Async.withTimeout(slowCall, Duration.ofMillis(200));
     * bounded.exceptionally(ex -> "fallback");
     * }</pre>
     *
     * @param future the future to bound with a timeout
     * @param timeout the maximum time to wait for {@code future} to complete
     * @param <T> the result type
     * @return a new future that mirrors {@code future}'s outcome, or fails with {@link
     *     AsyncTimeoutException} if {@code timeout} elapses first
     * @throws NullPointerException if {@code future} or {@code timeout} is {@code null}
     */
    public static <T> CompletableFuture<T> withTimeout(CompletableFuture<T> future, Duration timeout) {
        Validate.notNull(future, PARAM_FUTURE);
        Validate.notNull(timeout, PARAM_TIMEOUT);
        CompletableFuture<T> raced = future.orTimeout(timeout.toNanos(), TimeUnit.NANOSECONDS);
        CompletableFuture<T> result = new CompletableFuture<>();
        raced.whenComplete((value, throwable) -> {
            if (throwable == null) {
                result.complete(value);
                return;
            }
            Throwable cause = unwrapCompletionException(throwable);
            if (cause instanceof TimeoutException) {
                result.completeExceptionally(new AsyncTimeoutException(timeout, cause));
            } else {
                result.completeExceptionally(cause);
            }
        });
        return result;
    }

    /**
     * Validates {@code futures} and every element within it, and returns a defensive,
     * unmodifiable copy.
     *
     * @param futures the list to validate and copy
     * @param name the parameter name to use in exception messages
     * @return an unmodifiable copy of {@code futures}
     */
    private static <T> List<CompletableFuture<T>> copyRejectingNullElements(
            List<CompletableFuture<T>> futures, String name) {
        Validate.notNull(futures, name);
        return futures.stream()
                .map(future -> Validate.notNull(future, name + " element"))
                .collect(Collectors.toUnmodifiableList());
    }

    /**
     * Strips one layer of {@link CompletionException} wrapping, if present, to recover
     * the real underlying cause.
     *
     * @param throwable the throwable to unwrap
     * @return {@code throwable}'s cause if it is a {@link CompletionException} with a
     *     non-null cause, otherwise {@code throwable} itself
     */
    private static Throwable unwrapCompletionException(Throwable throwable) {
        if (throwable instanceof CompletionException && throwable.getCause() != null) {
            return throwable.getCause();
        }
        return throwable;
    }

    @SuppressWarnings("unchecked")
    private static <T> T castResult(Object value) {
        return (T) value;
    }
}
