package lithej.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Set;
import org.junit.jupiter.api.Test;

class SetsTest {

    @Test
    void ofCreatesImmutableSet() {
        Set<String> result = Sets.of("a", "b");
        assertThat(result).containsExactlyInAnyOrder("a", "b");
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> result.add("c"));
    }

    @Test
    void ofWithNoArgumentsIsEmpty() {
        assertThat(Sets.of()).isEmpty();
    }

    @Test
    void ofRejectsDuplicates() {
        assertThatIllegalArgumentException().isThrownBy(() -> Sets.of("a", "a"));
    }

    @Test
    void ofRejectsNullArray() {
        String[] items = null;
        assertThatNullPointerException().isThrownBy(() -> Sets.of(items));
    }

    @Test
    void ofRejectsNullElement() {
        assertThatNullPointerException().isThrownBy(() -> Sets.of("a", null));
    }

    @Test
    void mutableCreatesNewMutableSetDedupingInFirstOccurrenceOrder() {
        Set<String> result = Sets.mutable("a", "b", "a");
        result.add("c");
        assertThat(result).containsExactly("a", "b", "c");
    }

    @Test
    void mutableAllowsNullElements() {
        Set<String> result = Sets.mutable("a", null);
        assertThat(result).contains((String) null);
    }

    @Test
    void mutableRejectsNullArray() {
        String[] items = null;
        assertThatNullPointerException().isThrownBy(() -> Sets.mutable(items));
    }

    @Test
    void unionCombinesBothSets() {
        Set<Integer> result = Sets.union(Set.of(1, 2), Set.of(2, 3));
        assertThat(result).containsExactlyInAnyOrder(1, 2, 3);
    }

    @Test
    void unionWithEmptySetReturnsOther() {
        assertThat(Sets.union(Set.of(), Set.of(1, 2))).containsExactlyInAnyOrder(1, 2);
    }

    @Test
    void unionDoesNotMutateInputs() {
        Set<Integer> a = Sets.mutable(1, 2);
        Set<Integer> b = Sets.mutable(2, 3);
        Set<Integer> result = Sets.union(a, b);
        result.add(99);
        assertThat(a).containsExactlyInAnyOrder(1, 2);
        assertThat(b).containsExactlyInAnyOrder(2, 3);
    }

    @Test
    void unionRejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> Sets.union(null, Set.of()));
        assertThatNullPointerException().isThrownBy(() -> Sets.union(Set.of(), null));
    }

    @Test
    void intersectionKeepsElementsPresentInBoth() {
        Set<Integer> result = Sets.intersection(Set.of(1, 2, 3), Set.of(2, 3, 4));
        assertThat(result).containsExactlyInAnyOrder(2, 3);
    }

    @Test
    void intersectionWithDisjointSetsIsEmpty() {
        assertThat(Sets.intersection(Set.of(1), Set.of(2))).isEmpty();
    }

    @Test
    void intersectionRejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> Sets.intersection(null, Set.of()));
        assertThatNullPointerException().isThrownBy(() -> Sets.intersection(Set.of(), null));
    }

    @Test
    void differenceKeepsElementsOnlyInFirstSet() {
        Set<Integer> result = Sets.difference(Set.of(1, 2, 3), Set.of(2, 3));
        assertThat(result).containsExactlyInAnyOrder(1);
    }

    @Test
    void differenceWithIdenticalSetsIsEmpty() {
        assertThat(Sets.difference(Set.of(1, 2), Set.of(1, 2))).isEmpty();
    }

    @Test
    void differenceRejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> Sets.difference(null, Set.of()));
        assertThatNullPointerException().isThrownBy(() -> Sets.difference(Set.of(), null));
    }

    @Test
    void isSubsetOfDetectsSubsetsAndNonSubsets() {
        assertThat(Sets.isSubsetOf(Set.of(1, 2), Set.of(1, 2, 3))).isTrue();
        assertThat(Sets.isSubsetOf(Set.of(1, 4), Set.of(1, 2, 3))).isFalse();
    }

    @Test
    void isSubsetOfEmptySetIsAlwaysTrue() {
        assertThat(Sets.isSubsetOf(Set.of(), Set.of())).isTrue();
        assertThat(Sets.isSubsetOf(Set.of(), Set.of(1, 2))).isTrue();
    }

    @Test
    void isSubsetOfRejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> Sets.isSubsetOf(null, Set.of()));
        assertThatNullPointerException().isThrownBy(() -> Sets.isSubsetOf(Set.of(), null));
    }
}
