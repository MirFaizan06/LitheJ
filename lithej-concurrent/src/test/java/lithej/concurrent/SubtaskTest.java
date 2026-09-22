package lithej.concurrent;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class SubtaskTest {

    @Test
    void newSubtaskStartsInRunningState() {
        Subtask<String> subtask = new Subtask<>();
        assertThat(subtask.state()).isEqualTo(Subtask.State.RUNNING);
    }

    @Test
    void succeedTransitionsToSuccessAndExposesTheValue() {
        Subtask<String> subtask = new Subtask<>();
        subtask.succeed("done");
        assertThat(subtask.state()).isEqualTo(Subtask.State.SUCCESS);
        assertThat(subtask.get()).isEqualTo("done");
    }

    @Test
    void succeedAllowsNullValue() {
        Subtask<String> subtask = new Subtask<>();
        subtask.succeed(null);
        assertThat(subtask.state()).isEqualTo(Subtask.State.SUCCESS);
        assertThat(subtask.get()).isNull();
    }

    @Test
    void failTransitionsToFailedAndExposesTheCause() {
        Subtask<String> subtask = new Subtask<>();
        RuntimeException cause = new RuntimeException("x");
        subtask.fail(cause);
        assertThat(subtask.state()).isEqualTo(Subtask.State.FAILED);
        assertThat(subtask.exception()).isSameAs(cause);
    }

    @Test
    void cancelTransitionsToCancelled() {
        Subtask<String> subtask = new Subtask<>();
        subtask.cancel();
        assertThat(subtask.state()).isEqualTo(Subtask.State.CANCELLED);
    }

    @Test
    void toStringIncludesState() {
        Subtask<String> subtask = new Subtask<>();
        assertThat(subtask.toString()).contains("RUNNING");
    }
}
