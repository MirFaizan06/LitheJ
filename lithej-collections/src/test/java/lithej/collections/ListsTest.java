package lithej.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class ListsTest {

    @Test
    void ofCreatesImmutableListInOrder() {
        List<String> result = Lists.of("a", "b", "c");
        assertThat(result).containsExactly("a", "b", "c");
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> result.add("d"));
    }

    @Test
    void ofWithNoArgumentsIsEmpty() {
        assertThat(Lists.of()).isEmpty();
    }

    @Test
    void ofRejectsNullArray() {
        String[] items = null;
        assertThatNullPointerException().isThrownBy(() -> Lists.of(items));
    }

    @Test
    void ofRejectsNullElement() {
        assertThatNullPointerException().isThrownBy(() -> Lists.of("a", null, "c"));
    }

    @Test
    void mutableCreatesNewMutableList() {
        List<String> result = Lists.mutable("a", "b");
        result.add("c");
        assertThat(result).containsExactly("a", "b", "c");
    }

    @Test
    void mutableAllowsNullElements() {
        List<String> result = Lists.mutable("a", null, "c");
        assertThat(result).containsExactly("a", null, "c");
    }

    @Test
    void mutableRejectsNullArray() {
        String[] items = null;
        assertThatNullPointerException().isThrownBy(() -> Lists.mutable(items));
    }

    @Test
    void mapTransformsEachElement() {
        List<Integer> result = Lists.map(List.of("a", "bb", "ccc"), String::length);
        assertThat(result).containsExactly(1, 2, 3);
    }

    @Test
    void mapOnEmptySourceIsEmpty() {
        assertThat(Lists.map(List.of(), String::length)).isEmpty();
    }

    @Test
    void mapReturnsNewList() {
        List<String> source = new ArrayList<>(List.of("a"));
        List<String> result = Lists.map(source, String::toUpperCase);
        assertThat(result).isNotSameAs(source);
    }

    @Test
    void mapRejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> Lists.map(null, String::length));
        assertThatNullPointerException().isThrownBy(() -> Lists.map(List.of("a"), null));
    }

    @Test
    void filterKeepsMatchingElements() {
        List<Integer> result = Lists.filter(List.of(1, 2, 3, 4), x -> x % 2 == 0);
        assertThat(result).containsExactly(2, 4);
    }

    @Test
    void filterOnEmptySourceIsEmpty() {
        assertThat(Lists.filter(List.<Integer>of(), x -> true)).isEmpty();
    }

    @Test
    void filterRejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> Lists.filter(null, x -> true));
        assertThatNullPointerException().isThrownBy(() -> Lists.filter(List.of(1), null));
    }

    @Test
    void rejectKeepsNonMatchingElements() {
        List<Integer> result = Lists.reject(List.of(1, 2, 3, 4), x -> x % 2 == 0);
        assertThat(result).containsExactly(1, 3);
    }

    @Test
    void rejectOnEmptySourceIsEmpty() {
        assertThat(Lists.reject(List.<Integer>of(), x -> true)).isEmpty();
    }

    @Test
    void rejectRejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> Lists.reject(null, x -> true));
        assertThatNullPointerException().isThrownBy(() -> Lists.reject(List.of(1), null));
    }

    @Test
    void findReturnsFirstMatch() {
        Optional<Integer> result = Lists.find(List.of(1, 2, 3, 4), x -> x % 2 == 0);
        assertThat(result).contains(2);
    }

    @Test
    void findReturnsEmptyWhenNoMatch() {
        Optional<Integer> result = Lists.find(List.of(1, 3, 5), x -> x % 2 == 0);
        assertThat(result).isEmpty();
    }

    @Test
    void findOnEmptySourceIsEmpty() {
        assertThat(Lists.find(List.<Integer>of(), x -> true)).isEmpty();
    }

    @Test
    void findMatchingNullElementCollapsesToEmpty() {
        List<String> withNull = Lists.mutable("a", null, "c");
        Optional<String> result = Lists.find(withNull, x -> x == null);
        assertThat(result).isEmpty();
    }

    @Test
    void findRejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> Lists.find(null, x -> true));
        assertThatNullPointerException().isThrownBy(() -> Lists.find(List.of(1), null));
    }

    @Test
    void distinctPreservesFirstOccurrenceOrder() {
        List<Integer> result = Lists.distinct(List.of(3, 1, 2, 1, 3, 2));
        assertThat(result).containsExactly(3, 1, 2);
    }

    @Test
    void distinctOnEmptySourceIsEmpty() {
        assertThat(Lists.distinct(List.<Integer>of())).isEmpty();
    }

    @Test
    void distinctOnSingleElementReturnsIt() {
        assertThat(Lists.distinct(List.of(5))).containsExactly(5);
    }

    @Test
    void distinctRejectsNullSource() {
        assertThatNullPointerException().isThrownBy(() -> Lists.distinct(null));
    }

    @Test
    void flattenConcatenatesInOrder() {
        List<Integer> result = Lists.flatten(List.of(List.of(1, 2), List.of(), List.of(3)));
        assertThat(result).containsExactly(1, 2, 3);
    }

    @Test
    void flattenOnEmptySourceIsEmpty() {
        assertThat(Lists.flatten(List.<List<Integer>>of())).isEmpty();
    }

    @Test
    void flattenRejectsNullSource() {
        assertThatNullPointerException().isThrownBy(() -> Lists.flatten(null));
    }

    @Test
    void flattenRejectsNullInnerCollection() {
        List<List<Integer>> withNullInner = new ArrayList<>();
        withNullInner.add(List.of(1));
        withNullInner.add(null);
        assertThatNullPointerException().isThrownBy(() -> Lists.flatten(withNullInner));
    }

    @Test
    void chunkSplitsIntoExactMultiples() {
        List<List<Integer>> result = Lists.chunk(List.of(1, 2, 3, 4), 2);
        assertThat(result).containsExactly(List.of(1, 2), List.of(3, 4));
    }

    @Test
    void chunkLeavesSmallerFinalChunk() {
        List<List<Integer>> result = Lists.chunk(List.of(1, 2, 3, 4, 5), 2);
        assertThat(result).containsExactly(List.of(1, 2), List.of(3, 4), List.of(5));
    }

    @Test
    void chunkWithSizeLargerThanSourceReturnsOneChunk() {
        List<List<Integer>> result = Lists.chunk(List.of(1, 2), 10);
        assertThat(result).containsExactly(List.of(1, 2));
    }

    @Test
    void chunkOnEmptySourceIsEmpty() {
        assertThat(Lists.chunk(List.<Integer>of(), 3)).isEmpty();
    }

    @Test
    void chunkRejectsNonPositiveSize() {
        assertThatIllegalArgumentException().isThrownBy(() -> Lists.chunk(List.of(1), 0));
        assertThatIllegalArgumentException().isThrownBy(() -> Lists.chunk(List.of(1), -1));
    }

    @Test
    void chunkRejectsNullSource() {
        assertThatNullPointerException().isThrownBy(() -> Lists.chunk(null, 2));
    }

    @Test
    void chunksAreIndependentOfSource() {
        List<Integer> source = new ArrayList<>(List.of(1, 2, 3));
        List<List<Integer>> chunks = Lists.chunk(source, 2);
        chunks.get(0).add(99);
        assertThat(source).containsExactly(1, 2, 3);
    }

    @Test
    void partitionSplitsByPredicate() {
        Map<Boolean, List<Integer>> result = Lists.partition(List.of(1, 2, 3, 4), x -> x % 2 == 0);
        assertThat(result.get(Boolean.TRUE)).containsExactly(2, 4);
        assertThat(result.get(Boolean.FALSE)).containsExactly(1, 3);
    }

    @Test
    void partitionAlwaysHasBothKeys() {
        Map<Boolean, List<Integer>> result = Lists.partition(List.of(1, 3, 5), x -> x % 2 == 0);
        assertThat(result).containsKeys(Boolean.TRUE, Boolean.FALSE);
        assertThat(result.get(Boolean.TRUE)).isEmpty();
    }

    @Test
    void partitionOnEmptySourceHasBothEmptyLists() {
        Map<Boolean, List<Integer>> result = Lists.partition(List.<Integer>of(), x -> true);
        assertThat(result.get(Boolean.TRUE)).isEmpty();
        assertThat(result.get(Boolean.FALSE)).isEmpty();
    }

    @Test
    void partitionRejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> Lists.partition(null, x -> true));
        assertThatNullPointerException().isThrownBy(() -> Lists.partition(List.of(1), null));
    }

    @Test
    void zipPairsElementsByIndex() {
        List<Map.Entry<String, Integer>> result = Lists.zip(List.of("a", "b"), List.of(1, 2));
        assertThat(result).containsExactly(Map.entry("a", 1), Map.entry("b", 2));
    }

    @Test
    void zipTruncatesToShorterList() {
        List<Map.Entry<String, Integer>> result = Lists.zip(List.of("a", "b", "c"), List.of(1, 2));
        assertThat(result).containsExactly(Map.entry("a", 1), Map.entry("b", 2));

        List<Map.Entry<String, Integer>> reversed = Lists.zip(List.of("a"), List.of(1, 2, 3));
        assertThat(reversed).containsExactly(Map.entry("a", 1));
    }

    @Test
    void zipOnEitherEmptyListIsEmpty() {
        assertThat(Lists.zip(List.<String>of(), List.of(1, 2))).isEmpty();
        assertThat(Lists.zip(List.of("a"), List.<Integer>of())).isEmpty();
    }

    @Test
    void zipAllowsNullElements() {
        List<String> as = Lists.mutable("a", null);
        List<Integer> bs = Lists.mutable(1, null);
        List<Map.Entry<String, Integer>> result = Lists.zip(as, bs);
        assertThat(result).containsExactly(
                new AbstractMap.SimpleImmutableEntry<>("a", 1),
                new AbstractMap.SimpleImmutableEntry<>(null, null));
    }

    @Test
    void zipRejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> Lists.zip(null, List.of(1)));
        assertThatNullPointerException().isThrownBy(() -> Lists.zip(List.of("a"), null));
    }

    @Test
    void groupByGroupsElementsByKey() {
        Map<Integer, List<String>> result = Lists.groupBy(List.of("a", "bb", "cc", "ddd"), String::length);
        assertThat(result.keySet()).containsExactly(1, 2, 3);
        assertThat(result.get(2)).containsExactly("bb", "cc");
    }

    @Test
    void groupByOnEmptySourceIsEmpty() {
        assertThat(Lists.groupBy(List.<String>of(), String::length)).isEmpty();
    }

    @Test
    void groupByRejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> Lists.groupBy(null, String::length));
        assertThatNullPointerException().isThrownBy(() -> Lists.groupBy(List.of("a"), null));
    }

    @Test
    void countByCountsElementsPerKey() {
        Map<Integer, Long> result = Lists.countBy(List.of("a", "bb", "cc", "ddd"), String::length);
        assertThat(result).containsExactly(Map.entry(1, 1L), Map.entry(2, 2L), Map.entry(3, 1L));
    }

    @Test
    void countByOnEmptySourceIsEmpty() {
        assertThat(Lists.countBy(List.<String>of(), String::length)).isEmpty();
    }

    @Test
    void countByRejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> Lists.countBy(null, String::length));
        assertThatNullPointerException().isThrownBy(() -> Lists.countBy(List.of("a"), null));
    }

    @Test
    void sortByReturnsNewSortedListWithoutMutatingSource() {
        List<Integer> source = new ArrayList<>(List.of(3, 1, 2));
        List<Integer> result = Lists.sortBy(source, Comparator.naturalOrder());
        assertThat(result).containsExactly(1, 2, 3);
        assertThat(source).containsExactly(3, 1, 2);
    }

    @Test
    void sortByOnEmptySourceIsEmpty() {
        assertThat(Lists.sortBy(List.<Integer>of(), Comparator.naturalOrder())).isEmpty();
    }

    @Test
    void sortByRejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> Lists.sortBy(null, Comparator.naturalOrder()));
        assertThatNullPointerException().isThrownBy(() -> Lists.sortBy(List.of(1), null));
    }

    @Test
    void minByAndMaxByFindExtremes() {
        List<Integer> source = List.of(3, 1, 4, 1, 5);
        assertThat(Lists.minBy(source, Comparator.naturalOrder())).contains(1);
        assertThat(Lists.maxBy(source, Comparator.naturalOrder())).contains(5);
    }

    @Test
    void minByAndMaxByOnSingleElement() {
        assertThat(Lists.minBy(List.of(7), Comparator.naturalOrder())).contains(7);
        assertThat(Lists.maxBy(List.of(7), Comparator.naturalOrder())).contains(7);
    }

    @Test
    void minByAndMaxByOnEmptySourceAreEmpty() {
        assertThat(Lists.minBy(List.<Integer>of(), Comparator.naturalOrder())).isEmpty();
        assertThat(Lists.maxBy(List.<Integer>of(), Comparator.naturalOrder())).isEmpty();
    }

    @Test
    void minByWhenSmallestIsNullCollapsesToEmpty() {
        List<String> withNull = Lists.mutable("b", null, "a");
        Optional<String> result = Lists.minBy(withNull, Comparator.nullsFirst(Comparator.naturalOrder()));
        assertThat(result).isEmpty();
    }

    @Test
    void minByAndMaxByRejectNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> Lists.minBy(null, Comparator.naturalOrder()));
        assertThatNullPointerException().isThrownBy(() -> Lists.minBy(List.of(1), null));
        assertThatNullPointerException().isThrownBy(() -> Lists.maxBy(null, Comparator.naturalOrder()));
        assertThatNullPointerException().isThrownBy(() -> Lists.maxBy(List.of(1), null));
    }

    @Test
    void takeReturnsFirstNElements() {
        assertThat(Lists.take(List.of(1, 2, 3, 4), 2)).containsExactly(1, 2);
    }

    @Test
    void takeWithZeroIsEmpty() {
        assertThat(Lists.take(List.of(1, 2, 3), 0)).isEmpty();
    }

    @Test
    void takeWithNLargerThanSourceReturnsWholeSource() {
        assertThat(Lists.take(List.of(1, 2), 10)).containsExactly(1, 2);
    }

    @Test
    void takeWithExactSizeReturnsWholeSource() {
        assertThat(Lists.take(List.of(1, 2, 3), 3)).containsExactly(1, 2, 3);
    }

    @Test
    void takeRejectsNegativeN() {
        assertThatIllegalArgumentException().isThrownBy(() -> Lists.take(List.of(1), -1));
    }

    @Test
    void takeRejectsNullSource() {
        assertThatNullPointerException().isThrownBy(() -> Lists.take(null, 1));
    }

    @Test
    void dropReturnsAllButFirstNElements() {
        assertThat(Lists.drop(List.of(1, 2, 3, 4), 2)).containsExactly(3, 4);
    }

    @Test
    void dropWithZeroReturnsWholeSource() {
        assertThat(Lists.drop(List.of(1, 2, 3), 0)).containsExactly(1, 2, 3);
    }

    @Test
    void dropWithNLargerThanSourceIsEmpty() {
        assertThat(Lists.drop(List.of(1, 2), 10)).isEmpty();
    }

    @Test
    void dropWithExactSizeIsEmpty() {
        assertThat(Lists.drop(List.of(1, 2, 3), 3)).isEmpty();
    }

    @Test
    void dropRejectsNegativeN() {
        assertThatIllegalArgumentException().isThrownBy(() -> Lists.drop(List.of(1), -1));
    }

    @Test
    void dropRejectsNullSource() {
        assertThatNullPointerException().isThrownBy(() -> Lists.drop(null, 1));
    }

    @Test
    void takeAndDropReturnNewListsIndependentOfSource() {
        List<Integer> source = new ArrayList<>(List.of(1, 2, 3));
        List<Integer> taken = Lists.take(source, 2);
        List<Integer> dropped = Lists.drop(source, 1);
        taken.add(99);
        dropped.add(100);
        assertThat(source).containsExactly(1, 2, 3);
    }

    @Test
    void firstAndLastReturnEndpoints() {
        List<Integer> source = List.of(1, 2, 3);
        assertThat(Lists.first(source)).contains(1);
        assertThat(Lists.last(source)).contains(3);
    }

    @Test
    void firstAndLastOnSingleElementListReturnSameElement() {
        List<Integer> source = List.of(9);
        assertThat(Lists.first(source)).contains(9);
        assertThat(Lists.last(source)).contains(9);
    }

    @Test
    void firstAndLastOnEmptySourceAreEmpty() {
        assertThat(Lists.first(List.<Integer>of())).isEmpty();
        assertThat(Lists.last(List.<Integer>of())).isEmpty();
    }

    @Test
    void firstAndLastOnNullElementCollapseToEmpty() {
        List<String> singleNull = Arrays.asList((String) null);
        assertThat(Lists.first(singleNull)).isEmpty();
        assertThat(Lists.last(singleNull)).isEmpty();
    }

    @Test
    void firstAndLastRejectNullSource() {
        assertThatNullPointerException().isThrownBy(() -> Lists.first(null));
        assertThatNullPointerException().isThrownBy(() -> Lists.last(null));
    }
}
