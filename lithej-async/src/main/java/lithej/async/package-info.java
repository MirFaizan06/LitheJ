/**
 * Ergonomic helpers around {@link java.util.concurrent.CompletableFuture} and the JDK
 * {@link java.util.concurrent.Executor}/{@link java.util.concurrent.ExecutorService}
 * framework. {@link lithej.async.Async} removes common boilerplate (fan-out/fan-in,
 * blocking-with-timeout, non-blocking timeout) while keeping the underlying futures and
 * executors fully visible and controllable.
 *
 * <p>This is deliberately not a competing async framework or a reactive-streams
 * implementation: every method accepts and returns standard {@code java.util.concurrent}
 * types, and for anything beyond this small surface, drop straight down to
 * {@link java.util.concurrent.CompletableFuture} itself.
 *
 * <p><b>Executor discipline:</b> the single rule this package enforces is that callers
 * can always see and control which {@link java.util.concurrent.Executor} runs their
 * work. Every method that schedules asynchronous work either takes an {@link
 * java.util.concurrent.Executor} parameter or, in the one documented exception ({@link
 * lithej.async.Async#run(java.util.function.Supplier)}), says so explicitly in its
 * Javadoc and points to the executor-accepting overload.
 */
package lithej.async;
