/**
 * Leak-proof structured concurrency for virtual threads: {@link lithej.concurrent.Concurrency}
 * creates a {@link lithej.concurrent.TaskScope} that forks work onto virtual threads
 * and guarantees, structurally, that none of it can outlive the scope, with
 * zero-configuration virtual-thread pinning diagnostics
 * ({@link lithej.concurrent.PinningEvent}) built in. Built entirely on Java 21's
 * stable APIs, not the JDK's own preview {@code StructuredTaskScope}.
 */
package lithej.concurrent;
