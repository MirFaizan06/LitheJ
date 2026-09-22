/**
 * A safe wrapper around {@link java.lang.ProcessBuilder}: {@link lithej.process.ProcessX}
 * runs a command and returns a {@link lithej.process.ProcessResult} with its exit code
 * and fully-captured output, draining stdout/stderr concurrently to avoid pipe-buffer
 * deadlock. {@link lithej.process.ProcessOptions} configures working directory,
 * environment, and timeout.
 */
package lithej.process;
