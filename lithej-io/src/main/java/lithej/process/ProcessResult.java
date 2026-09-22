package lithej.process;

/**
 * The outcome of running an external process to completion: its exit code and the
 * fully-captured content of its standard output and standard error streams, decoded as
 * UTF-8.
 *
 * <p>Both {@code stdout} and {@code stderr} are captured entirely in memory. This is
 * appropriate for typical command-line tools whose output is at most a few megabytes;
 * for a process that streams gigabytes of output, use {@link java.lang.ProcessBuilder}
 * directly and consume its output incrementally instead.
 *
 * @param exitCode the process's exit code; conventionally {@code 0} means success
 * @param stdout everything the process wrote to standard output, as UTF-8
 * @param stderr everything the process wrote to standard error, as UTF-8
 */
public record ProcessResult(int exitCode, String stdout, String stderr) {

    /**
     * Returns {@code true} if {@link #exitCode()} is {@code 0}, the conventional
     * success code on every major platform.
     *
     * @return {@code true} if the process exited with code {@code 0}
     */
    public boolean succeeded() {
        return exitCode == 0;
    }
}
