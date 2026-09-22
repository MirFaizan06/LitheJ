package lithej.process;

import java.io.Serial;
import java.time.Duration;

/**
 * Thrown by {@link ProcessX} when a process does not exit within its configured
 * timeout. By the time this exception is thrown, the process has already been
 * forcibly terminated (via {@link Process#destroyForcibly()}).
 */
public final class ProcessTimeoutException extends ProcessExecutionException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception describing the timeout that was exceeded.
     *
     * @param timeout the configured timeout that elapsed
     */
    public ProcessTimeoutException(Duration timeout) {
        super("Process did not exit within " + timeout + "; it has been forcibly terminated", null);
    }
}
