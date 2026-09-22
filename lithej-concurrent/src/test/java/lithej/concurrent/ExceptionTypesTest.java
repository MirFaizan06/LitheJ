package lithej.concurrent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;

import java.util.List;
import org.junit.jupiter.api.Test;

class ExceptionTypesTest {

    @Test
    void taskFailedExceptionExposesMessageAndCause() {
        RuntimeException cause = new RuntimeException("cause");
        TaskFailedException exception = new TaskFailedException("wrapped", cause);

        assertThat(exception.getMessage()).isEqualTo("wrapped");
        assertThat(exception.getCause()).isSameAs(cause);
    }

    @Test
    void multipleTaskFailuresExceptionExposesAllFailuresAndUsesFirstAsCause() {
        RuntimeException first = new RuntimeException("first");
        RuntimeException second = new RuntimeException("second");

        MultipleTaskFailuresException exception =
                new MultipleTaskFailuresException("multiple", List.of(first, second));

        assertThat(exception.failures()).containsExactly(first, second);
        assertThat(exception.getCause()).isSameAs(first);
        assertThat(exception.getMessage()).isEqualTo("multiple");
    }

    @Test
    void multipleTaskFailuresExceptionRejectsEmptyFailureList() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> new MultipleTaskFailuresException("none", List.of()));
    }

    @Test
    void multipleTaskFailuresExceptionFailuresListIsUnmodifiable() {
        MultipleTaskFailuresException exception =
                new MultipleTaskFailuresException("x", List.of(new RuntimeException()));

        assertThat(exception.failures()).isUnmodifiable();
    }
}
