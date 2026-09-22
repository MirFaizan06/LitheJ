package lithej.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import lithej.core.Validate;

/**
 * Static helpers over plain {@link Iterable} sources, for callers that only have (or only
 * want to accept) an {@link Iterable} rather than forcing a {@link Collection}.
 *
 * <p><b>Null policy:</b> a {@code null} {@link Iterable} argument is always rejected with
 * a {@link NullPointerException} via {@link Validate#notNull}; it is never silently
 * treated as empty.
 *
 * <p><b>Thread safety:</b> this class is stateless and thread-safe.
 */
public final class Iterables {

    private static final String PARAM_SOURCE = "source";

    private Iterables() {
    }

    /**
     * Materializes {@code source} into a list.
     *
     * @param source the elements to collect
     * @param <T> the element type
     * @return a new, mutable list holding every element of {@code source}, in iteration
     *     order
     * @throws NullPointerException if {@code source} is {@code null}
     */
    public static <T> List<T> toList(Iterable<T> source) {
        Validate.notNull(source, PARAM_SOURCE);
        List<T> result = new ArrayList<>();
        for (T element : source) {
            result.add(element);
        }
        return result;
    }

    /**
     * Counts the elements of {@code source}.
     *
     * <p>If {@code source} is already a {@link Collection}, this simply returns
     * {@link Collection#size()}. Otherwise every element is visited once to count it,
     * an {@code O(n)} traversal.
     *
     * @param source the elements to count
     * @param <T> the element type
     * @return the number of elements in {@code source}
     * @throws NullPointerException if {@code source} is {@code null}
     */
    public static <T> int size(Iterable<T> source) {
        Validate.notNull(source, PARAM_SOURCE);
        if (source instanceof Collection<T> collection) {
            return collection.size();
        }
        int count = 0;
        for (T ignored : source) {
            count++;
        }
        return count;
    }

    /**
     * Returns {@code true} if {@code source} has no elements.
     *
     * <p>If {@code source} is already a {@link Collection}, this simply returns
     * {@link Collection#isEmpty()}. Otherwise it inspects the first element of the
     * iterator, an {@code O(1)} check that does not fully traverse {@code source}.
     *
     * @param source the elements to check
     * @param <T> the element type
     * @return {@code true} if {@code source} has no elements
     * @throws NullPointerException if {@code source} is {@code null}
     */
    public static <T> boolean isEmpty(Iterable<T> source) {
        Validate.notNull(source, PARAM_SOURCE);
        if (source instanceof Collection<T> collection) {
            return collection.isEmpty();
        }
        return !source.iterator().hasNext();
    }

    /**
     * Returns the first element of {@code source}, if any.
     *
     * @param source the elements to read from
     * @param <T> the element type
     * @return the first element, or {@link Optional#empty()} if {@code source} has no
     *     elements or its first element is itself {@code null}
     * @throws NullPointerException if {@code source} is {@code null}
     */
    public static <T> Optional<T> firstOf(Iterable<T> source) {
        Validate.notNull(source, PARAM_SOURCE);
        var iterator = source.iterator();
        return iterator.hasNext() ? Optional.ofNullable(iterator.next()) : Optional.empty();
    }
}
