package lithej.concurrent;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Duration;
import org.junit.jupiter.api.Test;

class ConcurrencyTest {

    @Test
    void scopeDefaultsToFailFastPolicy() {
        try (TaskScope scope = Concurrency.scope()) {
            scope.fork(() -> 1);
            scope.joinAll();
        }
        // FAIL_FAST behavior itself is covered by TaskScopeTest; this just confirms
        // the no-arg factory produces a usable scope.
    }

    @Test
    void scopeAcceptsAnExplicitPolicy() {
        try (TaskScope scope = Concurrency.scope(FailurePolicy.COLLECT_ALL)) {
            scope.fork(() -> 1);
            scope.joinAll();
        }
    }

    @Test
    void pinningEventRecordExposesItsComponents() {
        PinningEvent event = new PinningEvent("vthread-1", Duration.ofMillis(50), "some.Class.method");

        assertThat(event.threadName()).isEqualTo("vthread-1");
        assertThat(event.duration()).isEqualTo(Duration.ofMillis(50));
        assertThat(event.stackTraceSummary()).isEqualTo("some.Class.method");
    }
}
