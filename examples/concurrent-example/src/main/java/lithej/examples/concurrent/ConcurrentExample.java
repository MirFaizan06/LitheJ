package lithej.examples.concurrent;

import java.util.List;
import lithej.concurrent.Concurrency;
import lithej.concurrent.FailurePolicy;
import lithej.concurrent.MultipleTaskFailuresException;
import lithej.concurrent.PinningEvent;
import lithej.concurrent.Subtask;
import lithej.concurrent.TaskFailedException;
import lithej.concurrent.TaskScope;

/**
 * Demonstrates {@code lithej-concurrent}: forking work onto virtual threads with a
 * structural no-leak guarantee, both failure policies, and zero-configuration
 * pinning diagnostics. Requires Java 21+.
 */
public final class ConcurrentExample {

    private ConcurrentExample() {
    }

    public static void main(String[] args) throws InterruptedException {
        basicForkAndJoin();
        failFastCancelsSiblingsOnFirstFailure();
        collectAllReportsEveryFailure();
        pinningDiagnostics();
    }

    private static void basicForkAndJoin() {
        System.out.println("=== Basic fork + joinAll ===");
        try (TaskScope scope = Concurrency.scope()) {
            Subtask<String> a = scope.fork(() -> slowGreeting("Ada", 100));
            Subtask<String> b = scope.fork(() -> slowGreeting("Grace", 150));

            scope.joinAll(); // waits for both; guaranteed no leak on the way out

            System.out.println(a.get() + " / " + b.get());
        }
    }

    private static void failFastCancelsSiblingsOnFirstFailure() {
        System.out.println();
        System.out.println("=== FAIL_FAST: one failure cancels the other task ===");
        try (TaskScope scope = Concurrency.scope()) {
            Subtask<String> ok = scope.fork(() -> slowGreeting("Linus", 5_000));
            scope.fork(() -> {
                throw new IllegalStateException("simulated failure");
            });

            scope.joinAll();
        } catch (TaskFailedException e) {
            System.out.println("Caught expected failure: " + e.getCause());
        }
    }

    private static void collectAllReportsEveryFailure() {
        System.out.println();
        System.out.println("=== COLLECT_ALL: every failure is reported together ===");
        try (TaskScope scope = Concurrency.scope(FailurePolicy.COLLECT_ALL)) {
            scope.fork(() -> slowGreeting("Margaret", 20));
            scope.fork(() -> {
                throw new RuntimeException("failure one");
            });
            scope.fork(() -> {
                throw new RuntimeException("failure two");
            });

            try {
                scope.joinAll();
            } catch (MultipleTaskFailuresException e) {
                System.out.println(e.failures().size() + " task(s) failed:");
                for (Throwable failure : e.failures()) {
                    System.out.println("  - " + failure.getMessage());
                }
            }
        }
    }

    private static void pinningDiagnostics() {
        System.out.println();
        System.out.println("=== Pinning diagnostics ===");
        Object monitor = new Object();

        TaskScope scope = Concurrency.scope();
        try {
            scope.fork(() -> {
                synchronized (monitor) {
                    Thread.sleep(200);
                }
                return null;
            });
            scope.joinAll();
        } finally {
            scope.close(); // flushes JFR's buffered pinning events
        }

        List<PinningEvent> pinned = scope.pinningEvents();
        if (pinned.isEmpty()) {
            System.out.println("No pinning detected -- expected on JDK 24+ (JEP 491) where"
                    + " synchronized generally no longer pins virtual threads.");
        } else {
            for (PinningEvent event : pinned) {
                System.out.println("Pinned for " + event.duration() + " on " + event.threadName());
            }
        }
    }

    private static String slowGreeting(String name, long millis) throws InterruptedException {
        Thread.sleep(millis);
        return "Hello, " + name + "!";
    }
}
