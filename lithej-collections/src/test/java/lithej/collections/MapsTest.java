package lithej.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalStateException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.junit.jupiter.api.Test;

class MapsTest {

    @Test
    void invertSwapsKeysAndValues() {
        Map<Integer, String> source = new LinkedHashMap<>();
        source.put(1, "a");
        source.put(2, "b");
        Map<String, Integer> result = Maps.invert(source);
        assertThat(result).containsExactly(Map.entry("a", 1), Map.entry("b", 2));
    }

    @Test
    void invertOnEmptyMapIsEmpty() {
        assertThat(Maps.invert(Map.<String, Integer>of())).isEmpty();
    }

    @Test
    void invertOnSingleEntryMapWorks() {
        assertThat(Maps.invert(Map.of("k", 1))).containsExactly(Map.entry(1, "k"));
    }

    @Test
    void invertThrowsOnDuplicateValues() {
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("a", 1);
        source.put("b", 1);
        assertThatIllegalStateException().isThrownBy(() -> Maps.invert(source));
    }

    @Test
    void invertRejectsNullSource() {
        assertThatNullPointerException().isThrownBy(() -> Maps.invert(null));
    }

    @Test
    void filterKeysKeepsMatchingKeys() {
        Map<String, Integer> result = Maps.filterKeys(Map.of("a", 1, "bb", 2), k -> k.length() == 1);
        assertThat(result).containsExactly(Map.entry("a", 1));
    }

    @Test
    void filterKeysOnEmptyMapIsEmpty() {
        assertThat(Maps.filterKeys(Map.<String, Integer>of(), k -> true)).isEmpty();
    }

    @Test
    void filterKeysRejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> Maps.filterKeys(null, k -> true));
        assertThatNullPointerException().isThrownBy(() -> Maps.filterKeys(Map.of("a", 1), null));
    }

    @Test
    void filterValuesKeepsMatchingValues() {
        Map<String, Integer> result = Maps.filterValues(Map.of("a", 1, "b", 2), v -> v % 2 == 0);
        assertThat(result).containsExactly(Map.entry("b", 2));
    }

    @Test
    void filterValuesOnEmptyMapIsEmpty() {
        assertThat(Maps.filterValues(Map.<String, Integer>of(), v -> true)).isEmpty();
    }

    @Test
    void filterValuesRejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> Maps.filterValues(null, v -> true));
        assertThatNullPointerException().isThrownBy(() -> Maps.filterValues(Map.of("a", 1), null));
    }

    @Test
    void mapValuesTransformsValuesKeepingKeys() {
        Map<String, Integer> source = new LinkedHashMap<>();
        source.put("a", 1);
        source.put("b", 2);
        Map<String, Integer> result = Maps.mapValues(source, v -> v * 10);
        assertThat(result).containsExactly(Map.entry("a", 10), Map.entry("b", 20));
    }

    @Test
    void mapValuesOnEmptyMapIsEmpty() {
        assertThat(Maps.mapValues(Map.<String, Integer>of(), v -> v)).isEmpty();
    }

    @Test
    void mapValuesRejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> Maps.mapValues(null, v -> v));
        assertThatNullPointerException().isThrownBy(() -> Maps.mapValues(Map.of("a", 1), null));
    }

    @Test
    void mergeCombinesUniqueKeysAndResolvesConflictsWithSecondWinning() {
        Map<String, Integer> first = new LinkedHashMap<>();
        first.put("a", 1);
        first.put("b", 2);
        Map<String, Integer> second = new LinkedHashMap<>();
        second.put("b", 20);
        second.put("c", 3);
        Map<String, Integer> result = Maps.merge(first, second, (fromFirst, fromSecond) -> fromSecond);
        assertThat(result).containsExactly(Map.entry("a", 1), Map.entry("b", 20), Map.entry("c", 3));
    }

    @Test
    void mergeCanPreferFirstOnConflict() {
        Map<String, Integer> result = Maps.merge(
                Map.of("a", 1), Map.of("a", 2), (fromFirst, fromSecond) -> fromFirst);
        assertThat(result).containsExactly(Map.entry("a", 1));
    }

    @Test
    void mergeWithBothEmptyIsEmpty() {
        assertThat(Maps.merge(Map.of(), Map.of(), Integer::sum)).isEmpty();
    }

    @Test
    void mergeRejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> Maps.merge(null, Map.of(), Integer::sum));
        assertThatNullPointerException().isThrownBy(() -> Maps.merge(Map.of(), null, Integer::sum));
        assertThatNullPointerException().isThrownBy(() -> Maps.merge(Map.of(), Map.of(), null));
    }

    @Test
    void groupByGroupsSourceIntoMultiMap() {
        Map<Integer, List<String>> result =
                Maps.groupBy(List.of("a", "bb", "cc", "ddd"), String::length, Function.identity());
        assertThat(result.keySet()).containsExactly(1, 2, 3);
        assertThat(result.get(2)).containsExactly("bb", "cc");
    }

    @Test
    void groupByOnEmptySourceIsEmpty() {
        assertThat(Maps.groupBy(List.<String>of(), String::length, Function.identity())).isEmpty();
    }

    @Test
    void groupByRejectsNullArguments() {
        assertThatNullPointerException()
                .isThrownBy(() -> Maps.groupBy(null, String::length, Function.identity()));
        assertThatNullPointerException()
                .isThrownBy(() -> Maps.groupBy(List.of("a"), null, Function.identity()));
        assertThatNullPointerException()
                .isThrownBy(() -> Maps.groupBy(List.of("a"), String::length, null));
    }
}
