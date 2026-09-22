package lithej.process;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;
import lithej.core.Validate;

/**
 * A safe wrapper around {@link ProcessBuilder} for running external commands and
 * capturing their output.
 *
 * <p>Unlike calling {@link ProcessBuilder} directly, {@code ProcessX} always drains
 * standard output and standard error on dedicated threads while waiting for the
 * process to exit. This avoids a classic deadlock: if a process writes more output
 * than the OS pipe buffer holds (typically 64KB) and nothing is reading it, the
 * process blocks trying to write, and a caller that waits for exit before reading
 * output blocks forever waiting for a process that will never finish.
 *
 * <p><b>Quoting and platform semantics:</b> each element of the command list is passed
 * to the operating system as a single, separate argument (there is no shell involved
 * unless you explicitly invoke one, e.g. {@code List.of("sh", "-c", "...")} or
 * {@code List.of("cmd", "/c", "...")|}). This means you do <b>not</b> need to quote
 * arguments containing spaces — {@code List.of("git", "commit", "-m", "a message")}
 * passes the four-word message as a single argument, exactly as
 * {@link ProcessBuilder} itself would. Note that on Windows, the JDK reconstructs a
 * single command-line string internally using its own quoting rules, which can behave
 * unexpectedly with arguments containing embedded quotes; consult
 * {@link ProcessBuilder}'s documentation if you hit edge cases there.
 *
 * <p><b>Thread safety:</b> this class is stateless and thread-safe; each call to
 * {@code run} starts and fully manages its own process and threads.
 */
public final class ProcessX {

    private ProcessX() {
    }

    /**
     * Runs {@code command} with default options (inherited working directory and
     * environment, no timeout), waiting for it to exit.
     *
     * @param command the command and its arguments, e.g. {@code "git", "status"}
     * @return the process's exit code and captured output
     * @throws NullPointerException if {@code command} is {@code null} or contains a
     *     {@code null} element
     * @throws IllegalArgumentException if {@code command} is empty
     * @throws ProcessExecutionException if the process cannot be started, or the
     *     calling thread is interrupted while waiting for it to exit
     */
    public static ProcessResult run(String... command) {
        return run(List.of(command), ProcessOptions.defaults());
    }

    /**
     * Runs {@code command} with default options (inherited working directory and
     * environment, no timeout), waiting for it to exit.
     *
     * @param command the command and its arguments, e.g. {@code List.of("git", "status")}
     * @return the process's exit code and captured output
     * @throws NullPointerException if {@code command} is {@code null} or contains a
     *     {@code null} element
     * @throws IllegalArgumentException if {@code command} is empty
     * @throws ProcessExecutionException if the process cannot be started, or the
     *     calling thread is interrupted while waiting for it to exit
     */
    public static ProcessResult run(List<String> command) {
        return run(command, ProcessOptions.defaults());
    }

    /**
     * Runs {@code command} with the given {@code options}, waiting for it to exit (or
     * for {@code options}'s timeout to elapse, if one is set).
     *
     * <pre>{@code
     * ProcessResult result = ProcessX.run(
     *         List.of("git", "status"),
     *         ProcessOptions.defaults().withTimeout(Duration.ofSeconds(10)));
     * if (!result.succeeded()) {
     *     throw new IllegalStateException("git status failed: " + result.stderr());
     * }
     * }</pre>
     *
     * @param command the command and its arguments
     * @param options working directory, extra environment variables, and timeout
     * @return the process's exit code and captured output
     * @throws NullPointerException if either argument is {@code null}, or
     *     {@code command} contains a {@code null} element
     * @throws IllegalArgumentException if {@code command} is empty
     * @throws ProcessExecutionException if the process cannot be started, or the
     *     calling thread is interrupted while waiting for it to exit
     * @throws ProcessTimeoutException if {@code options} has a timeout and the process
     *     does not exit within it; the process is forcibly terminated before this is
     *     thrown
     */
    public static ProcessResult run(List<String> command, ProcessOptions options) {
        Validate.notEmpty(command, "command");
        Validate.notNull(options, "options");

        ProcessBuilder builder = new ProcessBuilder(command);
        if (options.workingDirectory() != null) {
            builder.directory(options.workingDirectory().toFile());
        }
        builder.environment().putAll(options.environment());

        Process process = start(builder, command);
        StreamGobbler stdout = StreamGobbler.start(process.getInputStream());
        StreamGobbler stderr = StreamGobbler.start(process.getErrorStream());

        awaitExit(process, options.timeout());
        stdout.join();
        stderr.join();

        return new ProcessResult(process.exitValue(), stdout.text(), stderr.text());
    }

    private static Process start(ProcessBuilder builder, List<String> command) {
        try {
            return builder.start();
        } catch (IOException e) {
            throw new ProcessExecutionException("Failed to start process: " + command, e);
        }
    }

    private static void awaitExit(Process process, Duration timeout) {
        try {
            boolean exited = timeout == null
                    ? awaitIndefinitely(process)
                    : process.waitFor(timeout.toMillis(), TimeUnit.MILLISECONDS);
            if (!exited) {
                process.destroyForcibly();
                throw new ProcessTimeoutException(timeout);
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            process.destroyForcibly();
            throw new ProcessExecutionException("Interrupted while waiting for process to exit", e);
        }
    }

    private static boolean awaitIndefinitely(Process process) throws InterruptedException {
        process.waitFor();
        return true;
    }

    /**
     * Drains an {@link InputStream} to completion on a dedicated daemon thread,
     * decoding the result as UTF-8.
     *
     * <p>{@code text} is written only by the gobbler thread and read only after
     * {@link #join()} returns, so the happens-before edge established by
     * {@link Thread#join()} is sufficient for visibility; no additional
     * synchronization or {@code volatile} is needed.
     */
    private static final class StreamGobbler {
        private final Thread thread;
        private String text = "";

        private StreamGobbler(InputStream input) {
            this.thread = new Thread(() -> read(input), "lithej-process-gobbler");
            this.thread.setDaemon(true);
        }

        static StreamGobbler start(InputStream input) {
            StreamGobbler gobbler = new StreamGobbler(input);
            gobbler.thread.start();
            return gobbler;
        }

        private void read(InputStream input) {
            try {
                text = new String(input.readAllBytes(), StandardCharsets.UTF_8);
            } catch (IOException e) {
                text = "";
            }
        }

        void join() {
            try {
                thread.join();
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }

        String text() {
            return text;
        }
    }
}
