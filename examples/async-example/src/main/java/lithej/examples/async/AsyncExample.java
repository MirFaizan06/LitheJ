package lithej.examples.async;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import lithej.async.Async;

/**
 * Demonstrates {@link Async}: running work on a caller-supplied, bounded executor
 * (never an implicit unbounded pool), combining several futures with {@link Async#all},
 * and blocking with a timeout via {@link Async#await}.
 */
public final class AsyncExample {

    private AsyncExample() {
    }

    public static void main(String[] args) {
        ExecutorService executor = Executors.newFixedThreadPool(4);
        try {
            List<CompletableFuture<Integer>> futures = List.of(
                    Async.run(() -> square(2), executor),
                    Async.run(() -> square(3), executor),
                    Async.run(() -> square(4), executor));

            CompletableFuture<List<Integer>> combined = Async.all(futures);
            List<Integer> results = Async.await(combined, Duration.ofSeconds(5));

            System.out.println("Squares: " + results);
            System.out.println("Sum: " + results.stream().mapToInt(Integer::intValue).sum());
        } finally {
            executor.shutdown();
        }
    }

    private static int square(int n) {
        return n * n;
    }
}
