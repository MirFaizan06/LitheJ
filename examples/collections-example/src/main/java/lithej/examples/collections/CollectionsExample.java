package lithej.examples.collections;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import lithej.collections.Lists;
import lithej.collections.Maps;

/**
 * Demonstrates {@link Lists} and {@link Maps}: filtering, chunking, grouping, and
 * inverting, all returning standard {@link java.util.List}/{@link java.util.Map}.
 */
public final class CollectionsExample {

    private CollectionsExample() {
    }

    public static void main(String[] args) {
        List<Integer> numbers = Lists.of(1, 2, 3, 4, 5, 6, 7, 8, 9, 10);

        List<Integer> even = Lists.filter(numbers, n -> n % 2 == 0);
        System.out.println("Even: " + even);

        List<List<Integer>> chunks = Lists.chunk(numbers, 3);
        System.out.println("Chunks of 3: " + chunks);

        Map<Boolean, List<Integer>> split = Lists.partition(numbers, n -> n > 5);
        System.out.println("Partitioned (>5): " + split);

        Map<String, List<Integer>> byParity =
                Lists.groupBy(numbers, n -> n % 2 == 0 ? "even" : "odd");
        System.out.println("Grouped by parity: " + byParity);

        Optional<Integer> max = Lists.maxBy(numbers, Comparator.naturalOrder());
        System.out.println("Max: " + max.orElse(-1));

        Map<String, Integer> scoreByName = Map.of("alice", 90, "bob", 75, "carol", 88);
        Map<Integer, String> nameByScore = Maps.invert(scoreByName);
        System.out.println("Inverted: " + nameByScore);

        Map<String, Integer> passing = Maps.filterValues(scoreByName, score -> score >= 80);
        System.out.println("Passing (>=80): " + passing);
    }
}
