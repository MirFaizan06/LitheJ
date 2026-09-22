package lithej.process;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.nio.file.Path;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class ProcessOptionsTest {

    @Test
    void defaultsHaveNoWorkingDirectoryEmptyEnvironmentAndNoTimeout() {
        ProcessOptions options = ProcessOptions.defaults();
        assertThat(options.workingDirectory()).isNull();
        assertThat(options.environment()).isEmpty();
        assertThat(options.timeout()).isNull();
    }

    @Test
    void withWorkingDirectoryReturnsNewInstanceWithoutMutatingOriginal() {
        ProcessOptions original = ProcessOptions.defaults();
        ProcessOptions updated = original.withWorkingDirectory(Path.of("."));

        assertThat(original.workingDirectory()).isNull();
        assertThat(updated.workingDirectory()).isEqualTo(Path.of("."));
    }

    @Test
    void withWorkingDirectoryRejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> ProcessOptions.defaults().withWorkingDirectory(null));
    }

    @Test
    void withEnvironmentReturnsNewInstanceWithoutMutatingOriginal() {
        ProcessOptions original = ProcessOptions.defaults();
        ProcessOptions updated = original.withEnvironment(Map.of("K", "V"));

        assertThat(original.environment()).isEmpty();
        assertThat(updated.environment()).containsEntry("K", "V");
    }

    @Test
    void withEnvironmentRejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> ProcessOptions.defaults().withEnvironment(null));
    }

    @Test
    void withTimeoutReturnsNewInstance() {
        ProcessOptions updated = ProcessOptions.defaults().withTimeout(Duration.ofSeconds(5));
        assertThat(updated.timeout()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void withTimeoutRejectsNullZeroAndNegative() {
        assertThatNullPointerException().isThrownBy(() -> ProcessOptions.defaults().withTimeout(null));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ProcessOptions.defaults().withTimeout(Duration.ZERO));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> ProcessOptions.defaults().withTimeout(Duration.ofSeconds(-1)));
    }

    @Test
    void equalsAndHashCodeReflectAllFields() {
        ProcessOptions a = ProcessOptions.defaults().withTimeout(Duration.ofSeconds(1));
        ProcessOptions b = ProcessOptions.defaults().withTimeout(Duration.ofSeconds(1));
        ProcessOptions c = ProcessOptions.defaults().withTimeout(Duration.ofSeconds(2));

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(a).isNotEqualTo("not an options object");
        assertThat(a).isEqualTo(a);
    }

    @Test
    void toStringDoesNotThrowAndMentionsFields() {
        ProcessOptions options = ProcessOptions.defaults()
                .withWorkingDirectory(Path.of("."))
                .withEnvironment(Map.of("SECRET", "value"))
                .withTimeout(Duration.ofSeconds(1));
        assertThat(options.toString()).contains("ProcessOptions");
    }
}
