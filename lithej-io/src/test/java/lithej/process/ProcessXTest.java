package lithej.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.nio.file.Path;
import java.time.Duration;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.api.io.TempDir;

/**
 * Uses the JVM itself ({@code java -version}, or a tiny inline script) as the external
 * process under test, so these tests do not depend on any particular shell or platform
 * tool being installed, keeping them portable across Windows/Linux/macOS CI.
 */
class ProcessXTest {

    @TempDir
    Path tempDir;

    private static String javaBinary() {
        return Path.of(System.getProperty("java.home"), "bin", OS.WINDOWS.isCurrentOs() ? "java.exe" : "java")
                .toString();
    }

    @Test
    void runCapturesExitCodeAndStdout() {
        ProcessResult result = ProcessX.run(javaBinary(), "-version");
        // java -version writes to stderr on most JDK builds; just assert it ran and
        // produced *some* output on one of the two streams, plus a real exit code.
        assertThat(result.exitCode()).isZero();
        assertThat(result.succeeded()).isTrue();
        assertThat(result.stdout() + result.stderr()).isNotBlank();
    }

    @Test
    void runWithListOverloadUsesDefaultOptions() {
        ProcessResult result = ProcessX.run(List.of(javaBinary(), "-version"));
        assertThat(result.succeeded()).isTrue();
    }

    @Test
    void runReturnsNonZeroExitCodeForInvalidArguments() {
        ProcessResult result = ProcessX.run(javaBinary(), "--this-flag-does-not-exist");
        assertThat(result.succeeded()).isFalse();
        assertThat(result.exitCode()).isNotZero();
    }

    @Test
    void runWithWorkingDirectorySetsProcessCwd() throws Exception {
        Path script = tempDir.resolve("PrintCwd.java");
        writeJavaSource(script, "System.out.println(System.getProperty(\"user.dir\"));");

        ProcessResult result = ProcessX.run(
                List.of(javaBinary(), script.toString()),
                ProcessOptions.defaults().withWorkingDirectory(tempDir));

        assertThat(result.succeeded()).isTrue();
        // Compare canonicalized (toRealPath) forms on both sides, not raw strings: on
        // Windows, a spawned process can report its cwd using the legacy 8.3 short
        // path form (e.g. "RUNNER~1" for a long account name like "runneradmin" on
        // GitHub's hosted runners) even though it is the exact same directory as the
        // long-form path JUnit's @TempDir gives us. Canonicalizing both sides avoids a
        // false failure from that harmless textual difference.
        Path reportedCwd = Path.of(result.stdout().trim());
        assertThat(reportedCwd.toRealPath()).isEqualTo(tempDir.toRealPath());
    }

    @Test
    void runWithEnvironmentPassesExtraVariables() {
        Path script = tempDir.resolve("PrintEnv.java");
        writeJavaSource(script, "System.out.println(System.getenv(\"LITHEJ_TEST_VAR\"));");

        ProcessResult result = ProcessX.run(
                List.of(javaBinary(), script.toString()),
                ProcessOptions.defaults().withEnvironment(Map.of("LITHEJ_TEST_VAR", "hello-from-test")));

        assertThat(result.stdout().trim()).isEqualTo("hello-from-test");
    }

    @Test
    void runWithTimeoutThrowsAndKillsProcess() {
        Path script = tempDir.resolve("Sleep.java");
        writeJavaSource(script, "Thread.sleep(60_000);");

        assertThatExceptionOfType(ProcessTimeoutException.class).isThrownBy(() -> ProcessX.run(
                List.of(javaBinary(), script.toString()),
                ProcessOptions.defaults().withTimeout(Duration.ofMillis(500))));
    }

    @Test
    void runThrowsProcessExecutionExceptionWhenCommandCannotStart() {
        assertThatExceptionOfType(ProcessExecutionException.class)
                .isThrownBy(() -> ProcessX.run("this-command-does-not-exist-anywhere-12345"));
    }

    @Test
    void runRejectsEmptyCommand() {
        assertThatIllegalArgumentException().isThrownBy(() -> ProcessX.run(List.of()));
    }

    @Test
    void runRejectsNullCommand() {
        assertThatNullPointerException().isThrownBy(() -> ProcessX.run((List<String>) null));
    }

    @Test
    void processResultSucceededReflectsExitCode() {
        assertThat(new ProcessResult(0, "", "").succeeded()).isTrue();
        assertThat(new ProcessResult(1, "", "").succeeded()).isFalse();
    }

    private static void writeJavaSource(Path file, String bodyStatement) {
        String source = "class " + baseName(file) + " {\n"
                + "    public static void main(String[] args) throws Exception {\n"
                + "        " + bodyStatement + "\n"
                + "    }\n"
                + "}\n";
        try {
            java.nio.file.Files.writeString(file, source);
        } catch (java.io.IOException e) {
            throw new java.io.UncheckedIOException(e);
        }
    }

    private static String baseName(Path file) {
        String name = file.getFileName().toString();
        return name.substring(0, name.length() - ".java".length());
    }
}
