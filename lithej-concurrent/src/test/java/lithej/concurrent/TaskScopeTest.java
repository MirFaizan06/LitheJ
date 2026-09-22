package lithej.concurrent;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArraySet;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.Timeout;

class TaskScopeTest {

    @Test
    void forkAndJoinAllReturnsResultsInAnyOrderAccessibleViaSubtask() {
        try (TaskScope scope = Concurrency.scope()) {
            Subtask<Integer> a = scope.fork(() -> 1);
            Subtask<Integer> b = scope.fork(() -> 2);
            Subtask<Integer> c = scope.fork(() -> 3);

            scope.joinAll();

            assertThat(a.state()).isEqualTo(Subtask.State.SUCCESS);
            assertThat(b.state()).isEqualTo(Subtask.State.SUCCESS);
            assertThat(c.state()).isEqualTo(Subtask.State.SUCCESS);
            assertThat(a.get() + b.get() + c.get()).isEqualTo(6);
        }
    }

    @Test
    void emptyScopeJoinsAndClosesWithoutError() {
        try (TaskScope scope = Concurrency.scope()) {
            scope.joinAll();
        }
        // Also fine without ever calling joinAll():
        try (TaskScope scope = Concurrency.scope()) {
            assertThat(scope.pinningEvents()).isEmpty();
        }
    }

    @Test
    void forkRunsEachTaskOnItsOwnVirtualThread() {
        Set<Long> threadIds = new CopyOnWriteArraySet<>();
        try (TaskScope scope = Concurrency.scope()) {
            for (int i = 0; i < 20; i++) {
                scope.fork(() -> {
                    assertThat(Thread.currentThread().isVirtual()).isTrue();
                    threadIds.add(Thread.currentThread().threadId());
                    return null;
                });
            }
            scope.joinAll();
        }
        assertThat(threadIds).hasSize(20);
    }

    @Test
    void failFastCancelsSiblingsAndThrowsTaskFailedExceptionWrappingTheRealCause() {
        AtomicBoolean siblingSawInterruption = new AtomicBoolean(false);
        CountDownLatch siblingStarted = new CountDownLatch(1);
        RuntimeException boom = new IllegalStateException("boom");

        TaskFailedException thrown = null;
        try (TaskScope scope = Concurrency.scope()) {
            Subtask<Void> failing = scope.fork(() -> {
                siblingStarted.await();
                throw boom;
            });
            Subtask<Void> sibling = scope.fork(() -> {
                siblingStarted.countDown();
                try {
                    Thread.sleep(30_000);
                } catch (InterruptedException e) {
                    siblingSawInterruption.set(true);
                    throw e;
                }
                return null;
            });

            try {
                scope.joinAll();
            } catch (TaskFailedException e) {
                thrown = e;
            }

            assertThat(failing.state()).isEqualTo(Subtask.State.FAILED);
            assertThat(sibling.state()).isEqualTo(Subtask.State.CANCELLED);
        }

        assertThat(thrown).isNotNull();
        assertThat(thrown.getCause()).isSameAs(boom);
        assertThat(siblingSawInterruption).isTrue();
    }

    @Test
    void collectAllWaitsForEveryTaskAndReportsEveryFailureInForkOrder() {
        RuntimeException first = new RuntimeException("first");
        RuntimeException second = new RuntimeException("second");

        MultipleTaskFailuresException thrown = null;
        try (TaskScope scope = Concurrency.scope(FailurePolicy.COLLECT_ALL)) {
            Subtask<Integer> ok = scope.fork(() -> 42);
            scope.fork(() -> {
                throw first;
            });
            scope.fork(() -> {
                throw second;
            });

            try {
                scope.joinAll();
            } catch (MultipleTaskFailuresException e) {
                thrown = e;
            }

            assertThat(ok.state()).isEqualTo(Subtask.State.SUCCESS);
            assertThat(ok.get()).isEqualTo(42);
        }

        assertThat(thrown).isNotNull();
        assertThat(thrown.failures()).containsExactly(first, second);
        assertThat(thrown.getCause()).isSameAs(first);
    }

    @Test
    void collectAllWithNoFailuresDoesNotThrow() {
        try (TaskScope scope = Concurrency.scope(FailurePolicy.COLLECT_ALL)) {
            scope.fork(() -> 1);
            scope.fork(() -> 2);
            scope.joinAll();
        }
    }

    @Test
    void closeGuaranteesNoLeakedThreadsEvenWhenJoinAllIsNeverCalled() throws InterruptedException {
        CountDownLatch release = new CountDownLatch(1);
        AtomicBoolean taskFinished = new AtomicBoolean(false);

        try (TaskScope scope = Concurrency.scope()) {
            scope.fork(() -> {
                release.await();
                taskFinished.set(true);
                return null;
            });
            // Deliberately never call joinAll(). Release the task just before the
            // implicit close() below would otherwise block indefinitely.
            new Thread(() -> {
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                    Thread.currentThread().interrupt();
                }
                release.countDown();
            }).start();
        } // close() must still wait for the task here.

        assertThat(taskFinished).isTrue();
    }

    @Test
    void closeSurfacesFailureWhenJoinAllWasNeverCalled() {
        RuntimeException boom = new RuntimeException("never joined");

        assertThatExceptionOfType(TaskFailedException.class).isThrownBy(() -> {
            try (TaskScope scope = Concurrency.scope()) {
                scope.fork(() -> {
                    throw boom;
                });
                // joinAll() intentionally not called; close() must still report this.
            }
        }).withCause(boom);
    }

    @Test
    void closeDoesNotDoubleThrowWhenJoinAllAlreadyDid() {
        RuntimeException boom = new RuntimeException("boom");
        int[] caughtCount = {0};

        try {
            try (TaskScope scope = Concurrency.scope()) {
                scope.fork(() -> {
                    throw boom;
                });
                scope.joinAll();
            }
        } catch (TaskFailedException e) {
            caughtCount[0]++;
            assertThat(e.getSuppressed()).isEmpty();
        }

        assertThat(caughtCount[0]).isEqualTo(1);
    }

    @Test
    void aUserExceptionBetweenForkAndJoinTakesPriorityAndTaskFailureIsSuppressed() {
        RuntimeException taskFailure = new RuntimeException("task failure");
        RuntimeException userCodeFailure = new IllegalStateException("user code failure");

        RuntimeException caught = null;
        try {
            try (TaskScope scope = Concurrency.scope()) {
                scope.fork(() -> {
                    throw taskFailure;
                });
                throw userCodeFailure; // thrown before joinAll() is reached
            }
        } catch (RuntimeException e) {
            caught = e;
        }

        assertThat(caught).isSameAs(userCodeFailure);
        assertThat(caught.getSuppressed()).hasSize(1);
        assertThat(caught.getSuppressed()[0]).isInstanceOf(TaskFailedException.class)
                .hasCause(taskFailure);
    }

    @Test
    void aTaskThrowingAnErrorStillMarksTheSubtaskFailedAndCancelsSiblings() throws InterruptedException {
        CountDownLatch siblingStarted = new CountDownLatch(1);
        AtomicBoolean siblingCancelled = new AtomicBoolean(false);

        try (TaskScope scope = Concurrency.scope()) {
            Subtask<Void> erroring = scope.fork(() -> {
                siblingStarted.await();
                throw new AssertionError("deliberate test error");
            });
            Subtask<Void> sibling = scope.fork(() -> {
                siblingStarted.countDown();
                try {
                    Thread.sleep(30_000);
                } catch (InterruptedException e) {
                    siblingCancelled.set(true);
                    throw e;
                }
                return null;
            });

            // The Error propagates out of the erroring virtual thread as an uncaught
            // exception too (printed to stderr, same as any uncaught Error would be),
            // but it is still tracked as this scope's task failure: joinAll() throws
            // TaskFailedException wrapping it, exactly like a thrown Exception would.
            assertThatExceptionOfType(TaskFailedException.class)
                    .isThrownBy(scope::joinAll)
                    .satisfies(thrown -> assertThat(thrown.getCause()).isInstanceOf(AssertionError.class));

            assertThat(erroring.state()).isEqualTo(Subtask.State.FAILED);
            assertThat(erroring.exception()).isInstanceOf(AssertionError.class);
            assertThat(sibling.state()).isEqualTo(Subtask.State.CANCELLED);
        }

        assertThat(siblingCancelled).isTrue();
    }

    @Test
    void subtaskGetThrowsIllegalStateExceptionWhenNotSuccessful() {
        try (TaskScope scope = Concurrency.scope(FailurePolicy.COLLECT_ALL)) {
            Subtask<Void> failed = scope.fork(() -> {
                throw new RuntimeException("x");
            });
            try {
                scope.joinAll();
            } catch (MultipleTaskFailuresException ignored) {
                // expected; asserting on the subtask below
            }
            assertThatExceptionOfType(IllegalStateException.class).isThrownBy(failed::get);
        }
    }

    @Test
    void subtaskExceptionThrowsIllegalStateExceptionWhenNotFailed() {
        try (TaskScope scope = Concurrency.scope()) {
            Subtask<Integer> success = scope.fork(() -> 1);
            scope.joinAll();
            assertThatExceptionOfType(IllegalStateException.class).isThrownBy(success::exception);
        }
    }

    @Test
    void forkRejectsNullTask() {
        try (TaskScope scope = Concurrency.scope()) {
            assertThatNullPointerException().isThrownBy(() -> scope.fork(null));
        }
    }

    @Test
    void concurrencyScopeRejectsNullPolicy() {
        assertThatNullPointerException().isThrownBy(() -> Concurrency.scope(null));
    }

    @Test
    void forkCanBeCalledConcurrentlyFromMultipleThreadsSafely() throws InterruptedException {
        int forkingThreads = 8;
        int tasksPerThread = 25;
        AtomicInteger totalForked = new AtomicInteger();
        CountDownLatch ready = new CountDownLatch(forkingThreads);
        CountDownLatch go = new CountDownLatch(1);

        try (TaskScope scope = Concurrency.scope()) {
            List<Thread> forkers = new ArrayList<>();
            for (int i = 0; i < forkingThreads; i++) {
                Thread forker = new Thread(() -> {
                    ready.countDown();
                    try {
                        go.await();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    for (int j = 0; j < tasksPerThread; j++) {
                        scope.fork(() -> {
                            totalForked.incrementAndGet();
                            return null;
                        });
                    }
                });
                forkers.add(forker);
                forker.start();
            }
            ready.await();
            go.countDown();
            for (Thread forker : forkers) {
                forker.join();
            }
            scope.joinAll();
        }

        assertThat(totalForked.get()).isEqualTo(forkingThreads * tasksPerThread);
    }

    @Test
    @Timeout(value = 15, unit = TimeUnit.SECONDS)
    void interruptingTheJoiningThreadStillJoinsTheChildAndRestoresInterruptStatus() throws InterruptedException {
        // Interrupting the thread blocked in joinAll() triggers this scope's own
        // FAIL_FAST cancellation (joinAllThreads() calls triggerCancellation() when it
        // catches InterruptedException) -- so the forked task ends up CANCELLED, not
        // completed normally. That's the correct, expected outcome, not a race: this
        // test asserts on that outcome directly instead of racing a separate release
        // signal against cancellation, which was the flaw in an earlier version of
        // this test (it assumed the task would still finish normally afterward).
        CountDownLatch taskStarted = new CountDownLatch(1);
        CountDownLatch aboutToJoin = new CountDownLatch(1);
        AtomicReference<Subtask<Void>> subtaskRef = new AtomicReference<>();
        AtomicBoolean joinerInterruptStatusRestored = new AtomicBoolean(false);

        Thread joiningThread = new Thread(() -> {
            try (TaskScope scope = Concurrency.scope()) {
                subtaskRef.set(scope.fork(() -> {
                    taskStarted.countDown();
                    Thread.sleep(60_000);
                    return null;
                }));
                // Signals readiness right before the blocking call below, so the
                // interrupting thread does not race fork()'s own setup work (which
                // includes starting a JFR recording stream) -- without this, the
                // interrupt could land before joinAll() is even reached, which is a
                // materially different (and also-valid, but not what this test is
                // about) scenario than interrupting an in-progress join().
                aboutToJoin.countDown();
                scope.joinAll();
            } catch (TaskFailedException ignored) {
                // A cancelled task is not a failure, so joinAll() is not expected to
                // throw here; this catch only guards against silently swallowing
                // something unexpected if that assumption is ever wrong.
            } finally {
                joinerInterruptStatusRestored.set(Thread.currentThread().isInterrupted());
            }
        });

        joiningThread.start();
        taskStarted.await();
        aboutToJoin.await();
        // aboutToJoin.countDown() happens immediately before scope.joinAll(), but
        // "immediately before" is still a handful of method calls before the actual
        // blocking Thread.join(); give it a brief, generous margin to actually reach
        // that blocking call so the interrupt lands where this test intends it to.
        Thread.sleep(100);
        joiningThread.interrupt();
        joiningThread.join(); // must terminate promptly -- proves the wait was not leaked

        assertThat(joinerInterruptStatusRestored).isTrue();
        assertThat(subtaskRef.get().state()).isEqualTo(Subtask.State.CANCELLED);
    }
}
