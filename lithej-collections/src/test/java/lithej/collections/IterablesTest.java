package lithej.collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class IterablesTest {

    /**
     * A bare {@link Iterable} (not a {@link java.util.Collection}) used to exercise the
     * fallback, iteration-based code paths in {@link Iterables}.
     */
    private static <T> Iterable<T> plainIterable(List<T> backing) {
        return () -> new Iterator<>() {
            private final Iterator<T> delegate = backing.iterator();

            @Override
            public boolean hasNext() {
                return delegate.hasNext();
            }

            @Override
            public T next() {
                if (!hasNext()) {
                    throw new NoSuchElementException();
                }
                return delegate.next();
            }
        };
    }

    @Test
    void toListMaterializesElementsInOrder() {
        List<Integer> result = Iterables.toList(plainIterable(List.of(1, 2, 3)));
        assertThat(result).containsExactly(1, 2, 3);
    }

    @Test
    void toListOnEmptyIterableIsEmpty() {
        assertThat(Iterables.toList(plainIterable(List.of()))).isEmpty();
    }

    @Test
    void toListReturnsNewMutableList() {
        List<Integer> result = Iterables.toList(plainIterable(List.of(1)));
        result.add(2);
        assertThat(result).containsExactly(1, 2);
    }

    @Test
    void toListRejectsNullSource() {
        assertThatNullPointerException().isThrownBy(() -> Iterables.toList(null));
    }

    @Test
    void sizeUsesCollectionSizeWhenAvailable() {
        assertThat(Iterables.size(List.of(1, 2, 3))).isEqualTo(3);
    }

    @Test
    void sizeCountsPlainIterableByTraversal() {
        assertThat(Iterables.size(plainIterable(List.of(1, 2, 3)))).isEqualTo(3);
    }

    @Test
    void sizeOnEmptyIterableIsZero() {
        assertThat(Iterables.size(plainIterable(List.of()))).isZero();
        assertThat(Iterables.size(List.of())).isZero();
    }

    @Test
    void sizeOnSingleElementIterableIsOne() {
        assertThat(Iterables.size(plainIterable(List.of("x")))).isEqualTo(1);
    }

    @Test
    void sizeRejectsNullSource() {
        assertThatNullPointerException().isThrownBy(() -> Iterables.size(null));
    }

    @Test
    void isEmptyUsesCollectionIsEmptyWhenAvailable() {
        assertThat(Iterables.isEmpty(List.of())).isTrue();
        assertThat(Iterables.isEmpty(List.of(1))).isFalse();
    }

    @Test
    void isEmptyChecksPlainIterableWithoutFullTraversal() {
        assertThat(Iterables.isEmpty(plainIterable(List.of()))).isTrue();
        assertThat(Iterables.isEmpty(plainIterable(List.of(1, 2)))).isFalse();
    }

    @Test
    void isEmptyRejectsNullSource() {
        assertThatNullPointerException().isThrownBy(() -> Iterables.isEmpty(null));
    }

    @Test
    void firstOfReturnsFirstElement() {
        assertThat(Iterables.firstOf(plainIterable(List.of(5, 6, 7)))).contains(5);
        assertThat(Iterables.firstOf(List.of(5, 6, 7))).contains(5);
    }

    @Test
    void firstOfOnSingleElementIterableReturnsIt() {
        assertThat(Iterables.firstOf(plainIterable(List.of(42)))).contains(42);
    }

    @Test
    void firstOfOnEmptyIterableIsEmpty() {
        assertThat(Iterables.firstOf(plainIterable(List.of()))).isEmpty();
        assertThat(Iterables.firstOf(List.of())).isEmpty();
    }

    @Test
    void firstOfOnNullFirstElementCollapsesToEmpty() {
        List<String> withNullFirst = new ArrayList<>();
        withNullFirst.add(null);
        withNullFirst.add("b");
        Optional<String> result = Iterables.firstOf(plainIterable(withNullFirst));
        assertThat(result).isEmpty();
    }

    @Test
    void firstOfRejectsNullSource() {
        assertThatNullPointerException().isThrownBy(() -> Iterables.firstOf(null));
    }
}
