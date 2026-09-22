package lithej.process;

import java.nio.file.Path;
import java.time.Duration;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import lithej.core.Validate;

/**
 * Immutable options controlling how {@link ProcessX} runs a command: working
 * directory, additional environment variables, and an optional timeout.
 *
 * <p>Each {@code withX} method returns a new instance; the receiver is not modified.
 * Start from {@link #defaults()} and layer on only the options you need:
 *
 * <pre>{@code
 * ProcessOptions options = ProcessOptions.defaults()
 *         .withWorkingDirectory(Path.of("/tmp/build"))
 *         .withTimeout(Duration.ofSeconds(30));
 * }</pre>
 *
 * <p><b>Thread safety:</b> immutable and thread-safe.
 */
public final class ProcessOptions {

    private static final ProcessOptions DEFAULTS = new ProcessOptions(null, Map.of(), null);

    private final Path workingDirectory;
    private final Map<String, String> environment;
    private final Duration timeout;

    private ProcessOptions(Path workingDirectory, Map<String, String> environment, Duration timeout) {
        this.workingDirectory = workingDirectory;
        this.environment = environment;
        this.timeout = timeout;
    }

    /**
     * Returns the default options: inherit the current working directory and
     * environment, with no timeout.
     *
     * @return the default options
     */
    public static ProcessOptions defaults() {
        return DEFAULTS;
    }

    /**
     * Returns a copy of this options object that runs the process in
     * {@code workingDirectory} instead of the JVM's current working directory.
     *
     * @param workingDirectory the directory the child process should run in
     * @return a new {@code ProcessOptions} with the given working directory
     * @throws NullPointerException if {@code workingDirectory} is {@code null}
     */
    public ProcessOptions withWorkingDirectory(Path workingDirectory) {
        Validate.notNull(workingDirectory, "workingDirectory");
        return new ProcessOptions(workingDirectory, environment, timeout);
    }

    /**
     * Returns a copy of this options object with additional environment variables set
     * for the child process. These are added on top of (and override, where names
     * collide) the JVM's own inherited environment; they do not replace it wholesale.
     *
     * @param environment environment variable names and values to set or override
     * @return a new {@code ProcessOptions} with the given environment variables added
     * @throws NullPointerException if {@code environment} is {@code null}
     */
    public ProcessOptions withEnvironment(Map<String, String> environment) {
        Validate.notNull(environment, "environment");
        return new ProcessOptions(workingDirectory, Map.copyOf(environment), timeout);
    }

    /**
     * Returns a copy of this options object that forcibly terminates the child
     * process if it has not exited within {@code timeout}.
     *
     * @param timeout the maximum time to wait for the process to exit
     * @return a new {@code ProcessOptions} with the given timeout
     * @throws NullPointerException if {@code timeout} is {@code null}
     * @throws IllegalArgumentException if {@code timeout} is zero or negative
     */
    public ProcessOptions withTimeout(Duration timeout) {
        Validate.notNull(timeout, "timeout");
        Validate.isTrue(!timeout.isZero() && !timeout.isNegative(), "timeout must be positive");
        return new ProcessOptions(workingDirectory, environment, timeout);
    }

    /**
     * Returns the configured working directory.
     *
     * @return the working directory, or {@code null} to inherit the JVM's current
     *     working directory
     */
    public Path workingDirectory() {
        return workingDirectory;
    }

    /**
     * Returns the configured additional environment variables.
     *
     * @return an immutable map of environment variables to add on top of the
     *     inherited environment; never {@code null}, possibly empty
     */
    public Map<String, String> environment() {
        return Map.copyOf(environment);
    }

    /**
     * Returns the configured timeout.
     *
     * @return the maximum time to wait for the process to exit, or {@code null} for
     *     no timeout
     */
    public Duration timeout() {
        return timeout;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (!(obj instanceof ProcessOptions other)) {
            return false;
        }
        return Objects.equals(workingDirectory, other.workingDirectory)
                && environment.equals(other.environment)
                && Objects.equals(timeout, other.timeout);
    }

    @Override
    public int hashCode() {
        return Objects.hash(workingDirectory, environment, timeout);
    }

    @Override
    public String toString() {
        Map<String, String> displayEnvironment = new LinkedHashMap<>(environment);
        return "ProcessOptions[workingDirectory=" + workingDirectory
                + ", environment=" + displayEnvironment.keySet()
                + ", timeout=" + timeout + "]";
    }
}
