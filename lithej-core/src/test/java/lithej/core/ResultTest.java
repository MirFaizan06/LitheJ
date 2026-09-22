package lithej.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNoException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ResultTest {

    @Test
    void successHoldsValue() {
        Result<Integer, String> result = Result.success(42);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.isFailure()).isFalse();
        assertThat(result.get()).isEqualTo(42);
    }

    @Test
    void successAllowsNullValue() {
        Result<String, String> result = Result.success(null);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isNull();
    }

    @Test
    void failureHoldsError() {
        Result<Integer, String> result = Result.failure("bad input");
        assertThat(result.isFailure()).isTrue();
        assertThat(result.isSuccess()).isFalse();
        assertThat(result.getError()).isEqualTo("bad input");
    }

    @Test
    void failureRejectsNullError() {
        assertThatNullPointerException().isThrownBy(() -> Result.failure(null));
    }

    @Test
    void getOnFailureThrows() {
        Result<Integer, String> result = Result.failure("bad");
        assertThatThrownBy(result::get).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getErrorOnSuccessThrows() {
        Result<Integer, String> result = Result.success(1);
        assertThatThrownBy(result::getError).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void orElseReturnsValueOnSuccessAndFallbackOnFailure() {
        assertThat(Result.<Integer, String>success(1).orElse(99)).isEqualTo(1);
        assertThat(Result.<Integer, String>failure("e").orElse(99)).isEqualTo(99);
    }

    @Test
    void orElseGetUsesErrorOnFailure() {
        Result<Integer, String> failure = Result.failure("bad");
        assertThat(failure.orElseGet(String::length)).isEqualTo(3);
        assertThat(Result.<Integer, String>success(7).orElseGet(e -> -1)).isEqualTo(7);
    }

    @Test
    void orElseThrowReturnsValueOnSuccess() {
        assertThatNoException().isThrownBy(() ->
                assertThat(Result.<Integer, String>success(5).orElseThrow(IllegalStateException::new)).isEqualTo(5));
    }

    @Test
    void orElseThrowThrowsMappedExceptionOnFailure() {
        Result<Integer, String> failure = Result.failure("bad");
        assertThatThrownBy(() -> failure.orElseThrow(IllegalStateException::new))
                .isInstanceOf(IllegalStateException.class)
                .hasMessage("bad");
    }

    @Test
    void mapTransformsSuccessValue() {
        Result<Integer, String> result = Result.<Integer, String>success(2).map(x -> x * 10);
        assertThat(result.get()).isEqualTo(20);
    }

    @Test
    void mapLeavesFailureUntouched() {
        Result<Integer, String> result = Result.<Integer, String>failure("e").map(x -> x * 10);
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("e");
    }

    @Test
    void mapErrorTransformsFailureValue() {
        Result<Integer, Integer> result = Result.<Integer, String>failure("bad").mapError(String::length);
        assertThat(result.getError()).isEqualTo(3);
    }

    @Test
    void mapErrorLeavesSuccessUntouched() {
        Result<Integer, Integer> result = Result.<Integer, String>success(5).mapError(String::length);
        assertThat(result.get()).isEqualTo(5);
    }

    @Test
    void flatMapChainsSuccess() {
        Result<Integer, String> result = Result.<Integer, String>success(2)
                .flatMap(x -> Result.success(x + 1))
                .flatMap(x -> Result.success(x * 10));
        assertThat(result.get()).isEqualTo(30);
    }

    @Test
    void flatMapShortCircuitsOnFailure() {
        Result<Integer, String> result = Result.<Integer, String>failure("e")
                .flatMap(x -> Result.success(x + 1));
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isEqualTo("e");
    }

    @Test
    void ifSuccessAndIfFailureInvokeCorrectCallback() {
        StringBuilder log = new StringBuilder();
        Result.<Integer, String>success(1).ifSuccess(v -> log.append("s").append(v));
        Result.<Integer, String>success(1).ifFailure(e -> log.append("should-not-run"));
        Result.<Integer, String>failure("e").ifFailure(e -> log.append("f").append(e));
        Result.<Integer, String>failure("e").ifSuccess(v -> log.append("should-not-run"));
        assertThat(log.toString()).isEqualTo("s1fe");
    }

    @Test
    void toOptionalReflectsSuccessOrFailure() {
        assertThat(Result.<Integer, String>success(1).toOptional()).contains(1);
        assertThat(Result.<Integer, String>failure("e").toOptional()).isEqualTo(Optional.empty());
    }

    @Test
    void toOptionalOnNullSuccessIsEmpty() {
        assertThat(Result.<String, String>success(null).toOptional()).isEmpty();
    }

    @Test
    void ofWrapsReturnedValueAsSuccess() {
        Result<String, Exception> result = Result.of(() -> "ok");
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("ok");
    }

    @Test
    void ofWrapsThrownExceptionAsFailure() {
        Result<String, Exception> result = Result.of(() -> {
            throw new IOException("disk error");
        });
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getError()).isInstanceOf(IOException.class).hasMessage("disk error");
    }

    @Test
    void successAndFailureSupportValueEquality() {
        assertThat(Result.success(1)).isEqualTo(Result.success(1));
        assertThat(Result.failure("e")).isEqualTo(Result.failure("e"));
        assertThat(Result.success(1)).isNotEqualTo(Result.success(2));
    }
}
