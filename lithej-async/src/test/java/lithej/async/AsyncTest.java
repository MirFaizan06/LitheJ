package lithej.async;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.io.IOException;
import java.time.Duration;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class AsyncTest {

    private ExecutorService executor;

    @BeforeEach
    void setUp() {
        executor = Executors.newFixedThreadPool(2);
    }

    @AfterEach
    void tearDown() {
        executor.shutdownNow();
    }

    // ---- run / runAsync ----

    @Test
    void runOnCommonPoolReturnsSuppliedValue() {
        CompletableFuture<Integer> future = Async.run(() -> 42);
        assertThat(future.join()).isEqualTo(42);
    }

    @Test
    void runWithCustomExecutorReturnsSuppliedValue() {
        CompletableFuture<Integer> future = Async.run(() -> 7, executor);
        assertThat(future.join()).isEqualTo(7);
    }

    @Test
    void runAsyncRunsRunnableOnGivenExecutor() throws InterruptedException {
        AtomicBoolean ran = new AtomicBoolean(false);
        CountDownLatch latch = new CountDownLatch(1);

        CompletableFuture<Void> future = Async.runAsync(() -> {
            ran.set(true);
            latch.countDown();
        }, executor);

        assertThat(latch.await(2, TimeUnit.SECONDS)).isTrue();
        assertThat(ran.get()).isTrue();
        assertThat(future.join()).isNull();
    }

    @Test
    void runRejectsNullTask() {
        assertThatNullPointerException().isThrownBy(() -> Async.<Object>run(null));
        assertThatNullPointerException().isThrownBy(() -> Async.<Object>run(null, executor));
    }

    @Test
    void runRejectsNullExecutor() {
        assertThatNullPointerException().isThrownBy(() -> Async.run(() -> 1, null));
    }

    @Test
    void runAsyncRejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> Async.runAsync(null, executor));
        assertThatNullPointerException().isThrownBy(() -> Async.runAsync(() -> { }, null));
    }

    // ---- all ----

    @Test
    void allWithEmptyListReturnsEmptyListImmediately() {
        CompletableFuture<List<Integer>> result = Async.<Integer>all(List.of());
        assertThat(result.isDone()).isTrue();
        assertThat(result.join()).isEmpty();
    }

    @Test
    void allReturnsResultsInInputOrderNotCompletionOrder() {
        CompletableFuture<String> firstInput = new CompletableFuture<>();
        CompletableFuture<String> secondInput = new CompletableFuture<>();
        CompletableFuture<List<String>> combined = Async.all(List.of(firstInput, secondInput));

        // Complete the SECOND input first to prove the output order tracks the input
        // list, not completion order.
        secondInput.complete("second-value");
        firstInput.complete("first-value");

        assertThat(combined.join()).containsExactly("first-value", "second-value");
    }

    @Test
    void allFailsWithFirstFailureCauseWhenAnyFutureFails() {
        CompletableFuture<String> okInput = CompletableFuture.completedFuture("ok");
        CompletableFuture<String> failingInput = new CompletableFuture<>();
        RuntimeException failure = new RuntimeException("lookup failed");

        CompletableFuture<List<String>> combined = Async.all(List.of(okInput, failingInput));
        failingInput.completeExceptionally(failure);

        assertThatThrownBy(combined::get)
                .isInstanceOf(ExecutionException.class)
                .cause().isSameAs(failure);
        assertThatThrownBy(combined::join)
                .isInstanceOf(CompletionException.class)
                .cause().isSameAs(failure);
    }

    @Test
    void allRejectsNullList() {
        assertThatNullPointerException().isThrownBy(() -> Async.<Object>all(null));
    }

    @Test
    void allRejectsNullElementInList() {
        List<CompletableFuture<Integer>> withNullElement = Collections.singletonList(null);
        assertThatNullPointerException().isThrownBy(() -> Async.all(withNullElement));
    }

    // ---- anyOf ----

    @Test
    void anyOfCompletesWithFirstSuccess() {
        CompletableFuture<String> a = new CompletableFuture<>();
        CompletableFuture<String> b = new CompletableFuture<>();
        a.complete("a-wins");

        CompletableFuture<String> race = Async.anyOf(List.of(a, b));

        assertThat(race.join()).isEqualTo("a-wins");
    }

    @Test
    void anyOfCompletesWithFirstFailure() {
        CompletableFuture<String> a = new CompletableFuture<>();
        CompletableFuture<String> b = new CompletableFuture<>();
        RuntimeException raceFailure = new RuntimeException("race failure");
        a.completeExceptionally(raceFailure);

        CompletableFuture<String> race = Async.anyOf(List.of(a, b));

        assertThatThrownBy(race::join)
                .isInstanceOf(CompletionException.class)
                .cause().isSameAs(raceFailure);
    }

    @Test
    void anyOfRejectsEmptyList() {
        assertThatIllegalArgumentException().isThrownBy(() -> Async.<Object>anyOf(List.of()));
    }

    @Test
    void anyOfRejectsNullList() {
        assertThatNullPointerException().isThrownBy(() -> Async.<Object>anyOf(null));
    }

    @Test
    void anyOfRejectsNullElementInList() {
        List<CompletableFuture<Integer>> withNullElement = Collections.singletonList(null);
        assertThatNullPointerException().isThrownBy(() -> Async.anyOf(withNullElement));
    }

    // ---- await ----

    @Test
    void awaitReturnsValueOnSuccess() {
        CompletableFuture<Integer> future = CompletableFuture.completedFuture(5);
        assertThat(Async.await(future, Duration.ofSeconds(1))).isEqualTo(5);
    }

    @Test
    void awaitRethrowsRuntimeExceptionCauseDirectly() {
        RuntimeException failure = new RuntimeException("unchecked failure");
        CompletableFuture<Integer> future = new CompletableFuture<>();
        future.completeExceptionally(failure);

        assertThatThrownBy(() -> Async.await(future, Duration.ofSeconds(1))).isSameAs(failure);
    }

    @Test
    void awaitRethrowsErrorCauseDirectly() {
        AssertionError failure = new AssertionError("unchecked error failure");
        CompletableFuture<Integer> future = new CompletableFuture<>();
        future.completeExceptionally(failure);

        assertThatThrownBy(() -> Async.await(future, Duration.ofSeconds(1))).isSameAs(failure);
    }

    @Test
    void awaitWrapsCheckedExceptionCauseInAsyncExecutionException() {
        IOException checkedFailure = new IOException("disk error");
        CompletableFuture<Integer> future = new CompletableFuture<>();
        future.completeExceptionally(checkedFailure);

        assertThatThrownBy(() -> Async.await(future, Duration.ofSeconds(1)))
                .isInstanceOf(AsyncExecutionException.class)
                .hasCause(checkedFailure);
    }

    @Test
    void awaitThrowsAsyncTimeoutExceptionWhenTimeoutElapses() {
        CompletableFuture<Integer> neverCompletes = new CompletableFuture<>();
        Duration timeout = Duration.ofMillis(100);

        assertThatThrownBy(() -> Async.await(neverCompletes, timeout))
                .isInstanceOf(AsyncTimeoutException.class)
                .hasMessageContaining(timeout.toString());
    }

    @Test
    void awaitRestoresInterruptFlagAndThrowsWhenInterrupted() {
        CompletableFuture<Integer> neverCompletes = new CompletableFuture<>();
        Thread.currentThread().interrupt();
        try {
            assertThatThrownBy(() -> Async.await(neverCompletes, Duration.ofMillis(500)))
                    .isInstanceOf(AsyncExecutionException.class)
                    .hasCauseInstanceOf(InterruptedException.class);
            assertThat(Thread.currentThread().isInterrupted()).isTrue();
        } finally {
            // Clear the interrupt flag so it doesn't bleed into later tests.
            assertThat(Thread.interrupted()).isTrue();
        }
    }

    @Test
    void awaitRejectsNullArguments() {
        CompletableFuture<Integer> future = CompletableFuture.completedFuture(1);
        assertThatNullPointerException().isThrownBy(() -> Async.<Object>await(null, Duration.ofSeconds(1)));
        assertThatNullPointerException().isThrownBy(() -> Async.await(future, null));
    }

    // ---- withTimeout ----

    @Test
    void withTimeoutResolvesNormallyWhenFutureCompletesInTime() {
        CompletableFuture<String> future = CompletableFuture.completedFuture("fast");
        CompletableFuture<String> bounded = Async.withTimeout(future, Duration.ofMillis(300));
        assertThat(bounded.join()).isEqualTo("fast");
    }

    @Test
    void withTimeoutFailsWithAsyncTimeoutExceptionWhenFutureIsTooSlow() {
        CompletableFuture<String> neverCompletes = new CompletableFuture<>();
        CompletableFuture<String> bounded = Async.withTimeout(neverCompletes, Duration.ofMillis(150));

        assertThatThrownBy(bounded::join)
                .isInstanceOf(CompletionException.class)
                .cause().isInstanceOf(AsyncTimeoutException.class);
    }

    @Test
    void withTimeoutMutatesUnderlyingFutureInPlaceAsDocumented() {
        CompletableFuture<String> neverCompletes = new CompletableFuture<>();
        Async.withTimeout(neverCompletes, Duration.ofMillis(150));

        // The Javadoc of withTimeout documents that orTimeout schedules the timeout on
        // the SAME future instance passed in, so any other holder of that reference
        // observes the exceptional completion too - verify that honestly here.
        assertThatThrownBy(neverCompletes::join)
                .isInstanceOf(CompletionException.class)
                .cause().isInstanceOf(TimeoutException.class);
    }

    @Test
    void withTimeoutPropagatesNonTimeoutFailureUnwrapped() {
        RuntimeException failure = new RuntimeException("not a timeout");
        // Fails via an async task (rather than a direct completeExceptionally) so the
        // failure is delivered internally wrapped in a CompletionException, exercising
        // the unwrapping this method documents.
        CompletableFuture<String> future = CompletableFuture.supplyAsync(() -> {
            throw failure;
        }, executor);
        CompletableFuture<String> bounded = Async.withTimeout(future, Duration.ofSeconds(5));

        assertThatThrownBy(bounded::join)
                .isInstanceOf(CompletionException.class)
                .cause().isSameAs(failure);
    }

    @Test
    void withTimeoutRejectsNullArguments() {
        CompletableFuture<Integer> future = CompletableFuture.completedFuture(1);
        assertThatNullPointerException().isThrownBy(() -> Async.<Object>withTimeout(null, Duration.ofSeconds(1)));
        assertThatNullPointerException().isThrownBy(() -> Async.withTimeout(future, null));
    }

    // ---- exception types ----

    @Test
    void asyncTimeoutExceptionWithoutCauseHasNullCauseAndMentionsTimeout() {
        Duration timeout = Duration.ofSeconds(2);
        AsyncTimeoutException exception = new AsyncTimeoutException(timeout);
        assertThat(exception.getMessage()).contains(timeout.toString());
        assertThat(exception.getCause()).isNull();
    }
}
