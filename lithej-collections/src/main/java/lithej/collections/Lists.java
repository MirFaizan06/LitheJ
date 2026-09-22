package lithej.collections;

import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Predicate;
import lithej.core.Validate;

/**
 * Static helpers over {@link List} that remove common boilerplate: mapping, filtering,
 * chunking, zipping, grouping, and simple aggregation.
 *
 * <p><b>Null policy:</b> a {@code null} collection/list argument is always rejected with
 * a {@link NullPointerException} via {@link Validate#notNull}; a {@code null} collection
 * is never silently treated as empty. A {@code null} function or predicate argument is
 * rejected the same way. Individual <em>elements</em> inside a source collection may be
 * {@code null} unless a specific method says otherwise; where a method returns a single
 * element wrapped in {@link Optional} (e.g. {@link #find}, {@link #first}, {@link #last}),
 * a matched {@code null} element collapses to {@link Optional#empty()}, exactly like
 * {@code Optional.ofNullable} â€” there is no way to distinguish "no match" from "matched a
 * null element" through an {@code Optional}, so pick a different method (e.g. manual
 * iteration) when that distinction matters.
 *
 * <p>Returned lists are new, mutable {@link ArrayList}s (so callers may freely modify
 * them without affecting the source) unless documented otherwise; {@link #of} is the one
 * exception, returning an immutable list akin to {@link List#of}.
 *
 * <p><b>Thread safety:</b> this class is stateless and thread-safe.
 */
public final class Lists {

    private static final String PARAM_SOURCE = "source";
    private static final String PARAM_PREDICATE = "predicate";

    private Lists() {
    }

    /**
     * Creates an immutable list containing {@code items}, in order, mirroring
     * {@link List#of(Object[])}.
     *
     * <pre>{@code
     * List<String> colors = Lists.of("red", "green", "blue");
     * }</pre>
     *
     * @param items the elements to include
     * @param <T> the element type
     * @return a new, immutable list containing {@code items}
     * @throws NullPointerException if {@code items} (the array) or any element of it is
     *     {@code null}
     */
    @SafeVarargs
    public static <T> List<T> of(T... items) {
        Validate.notNull(items, "items");
        return List.of(items);
    }

    /**
     * Creates a new mutable list containing {@code items}, in order.
     *
     * <p>Unlike {@link #of}, {@code null} elements are permitted, matching ordinary
     * {@link ArrayList} semantics.
     *
     * @param items the elements to include, may themselves be {@code null}
     * @param <T> the element type
     * @return a new, mutable {@link ArrayList} containing {@code items}
     * @throws NullPointerException if {@code items} (the array) is {@code null}
     */
    @SafeVarargs
    public static <T> List<T> mutable(T... items) {
        Validate.notNull(items, "items");
        return new ArrayList<>(Arrays.asList(items));
    }

    /**
     * Transforms every element of {@code source} with {@code mapper}.
     *
     * <pre>{@code
     * List<Integer> lengths = Lists.map(List.of("a", "bb", "ccc"), String::length);
     * // [1, 2, 3]
     * }</pre>
     *
     * @param source the elements to transform
     * @param mapper the transformation applied to each element
     * @param <T> the source element type
     * @param <R> the result element type
     * @return a new, mutable list holding {@code mapper.apply(element)} for each element
     *     of {@code source}, in iteration order
     * @throws NullPointerException if {@code source} or {@code mapper} is {@code null}
     */
    public static <T, R> List<R> map(Collection<T> source, Function<? super T, ? extends R> mapper) {
        Validate.notNull(source, PARAM_SOURCE);
        Validate.notNull(mapper, "mapper");
        List<R> result = new ArrayList<>(source.size());
        for (T element : source) {
            result.add(mapper.apply(element));
        }
        return result;
    }

    /**
     * Selects the elements of {@code source} that satisfy {@code predicate}.
     *
     * @param source the elements to filter
     * @param predicate the test applied to each element
     * @param <T> the element type
     * @return a new, mutable list holding the elements of {@code source} for which
     *     {@code predicate} returned {@code true}, in iteration order
     * @throws NullPointerException if {@code source} or {@code predicate} is {@code null}
     */
    public static <T> List<T> filter(Collection<T> source, Predicate<? super T> predicate) {
        Validate.notNull(source, PARAM_SOURCE);
        Validate.notNull(predicate, PARAM_PREDICATE);
        List<T> result = new ArrayList<>();
        for (T element : source) {
            if (predicate.test(element)) {
                result.add(element);
            }
        }
        return result;
    }

    /**
     * Selects the elements of {@code source} that do <em>not</em> satisfy
     * {@code predicate}; the inverse of {@link #filter}.
     *
     * @param source the elements to filter
     * @param predicate the test applied to each element
     * @param <T> the element type
     * @return a new, mutable list holding the elements of {@code source} for which
     *     {@code predicate} returned {@code false}, in iteration order
     * @throws NullPointerException if {@code source} or {@code predicate} is {@code null}
     */
    public static <T> List<T> reject(Collection<T> source, Predicate<? super T> predicate) {
        Validate.notNull(source, PARAM_SOURCE);
        Validate.notNull(predicate, PARAM_PREDICATE);
        return filter(source, predicate.negate());
    }

    /**
     * Finds the first element of {@code source} (in iteration order) that satisfies
     * {@code predicate}.
     *
     * @param source the elements to search
     * @param predicate the test applied to each element
     * @param <T> the element type
     * @return the first matching element, or {@link Optional#empty()} if none matches or
     *     the match itself is a {@code null} element
     * @throws NullPointerException if {@code source} or {@code predicate} is {@code null}
     */
    public static <T> Optional<T> find(Collection<T> source, Predicate<? super T> predicate) {
        Validate.notNull(source, PARAM_SOURCE);
        Validate.notNull(predicate, PARAM_PREDICATE);
        for (T element : source) {
            if (predicate.test(element)) {
                return Optional.ofNullable(element);
            }
        }
        return Optional.empty();
    }

    // Note: the spec suggested also shipping `findFirst(List<T>, Predicate<T>)` as an
    // alias of `find` for readability. It would do exactly the same thing as `find`
    // (which already works on any Collection, including List), so it was dropped rather
    // than shipping two names for one behavior.

    /**
     * Returns the distinct elements of {@code source}, preserving the order of first
     * occurrence.
     *
     * @param source the elements to deduplicate
     * @param <T> the element type
     * @return a new, mutable list holding each distinct element of {@code source} once,
     *     in the order it first appeared
     * @throws NullPointerException if {@code source} is {@code null}
     */
    public static <T> List<T> distinct(Collection<T> source) {
        Validate.notNull(source, PARAM_SOURCE);
        List<T> result = new ArrayList<>();
        for (T element : source) {
            if (!result.contains(element)) {
                result.add(element);
            }
        }
        return result;
    }

    /**
     * Concatenates the elements of every collection in {@code source}, in order.
     *
     * <pre>{@code
     * List<Integer> flat = Lists.flatten(List.of(List.of(1, 2), List.of(3), List.of()));
     * // [1, 2, 3]
     * }</pre>
     *
     * @param source a collection of collections to concatenate
     * @param <T> the element type
     * @return a new, mutable list holding every element of every inner collection, in
     *     the order the inner collections and their elements appear
     * @throws NullPointerException if {@code source} is {@code null}, or if {@code source}
     *     contains a {@code null} inner collection (a {@code null} collection cannot be
     *     iterated)
     */
    public static <T> List<T> flatten(Collection<? extends Collection<T>> source) {
        Validate.notNull(source, PARAM_SOURCE);
        List<T> result = new ArrayList<>();
        for (Collection<T> inner : source) {
            result.addAll(inner);
        }
        return result;
    }

    /**
     * Splits {@code source} into consecutive chunks of at most {@code size} elements
     * each. The final chunk may be smaller than {@code size} if {@code source.size()} is
     * not an exact multiple of it.
     *
     * <pre>{@code
     * List<List<Integer>> chunks = Lists.chunk(List.of(1, 2, 3, 4, 5), 2);
     * // [[1, 2], [3, 4], [5]]
     * }</pre>
     *
     * @param source the elements to split
     * @param size the maximum size of each chunk; must be {@code > 0}
     * @param <T> the element type
     * @return a new, mutable list of new, mutable chunks; empty if {@code source} is
     *     empty
     * @throws NullPointerException if {@code source} is {@code null}
     * @throws IllegalArgumentException if {@code size <= 0}
     */
    public static <T> List<List<T>> chunk(List<T> source, int size) {
        Validate.notNull(source, PARAM_SOURCE);
        if (size <= 0) {
            throw new IllegalArgumentException("size must be positive, was " + size);
        }
        List<List<T>> chunks = new ArrayList<>();
        for (int start = 0; start < source.size(); start += size) {
            int end = Math.min(start + size, source.size());
            chunks.add(new ArrayList<>(source.subList(start, end)));
        }
        return chunks;
    }

    /**
     * Splits {@code source} into two lists based on {@code predicate}, mirroring
     * {@link java.util.stream.Collectors#partitioningBy(Predicate)}.
     *
     * @param source the elements to partition
     * @param predicate the test applied to each element
     * @param <T> the element type
     * @return a new, mutable map with exactly two keys, {@link Boolean#TRUE} and
     *     {@link Boolean#FALSE}, each mapped to a new, mutable list; the {@code TRUE}
     *     list holds the elements for which {@code predicate} returned {@code true}, and
     *     the {@code FALSE} list holds the rest. Both keys are always present, even if
     *     one of the lists is empty.
     * @throws NullPointerException if {@code source} or {@code predicate} is {@code null}
     */
    public static <T> Map<Boolean, List<T>> partition(Collection<T> source, Predicate<? super T> predicate) {
        Validate.notNull(source, PARAM_SOURCE);
        Validate.notNull(predicate, PARAM_PREDICATE);
        Map<Boolean, List<T>> result = new LinkedHashMap<>();
        result.put(Boolean.TRUE, new ArrayList<>());
        result.put(Boolean.FALSE, new ArrayList<>());
        for (T element : source) {
            result.get(predicate.test(element)).add(element);
        }
        return result;
    }

    /**
     * Pairs up the elements of {@code as} and {@code bs} by index.
     *
     * <p>If the lists have different lengths, the result is truncated to the length of
     * the shorter list; the extra elements of the longer list are silently dropped
     * rather than causing an error.
     *
     * <pre>{@code
     * List<Map.Entry<String, Integer>> pairs = Lists.zip(List.of("a", "b", "c"), List.of(1, 2));
     * // [a=1, b=2]
     * }</pre>
     *
     * @param as the first list
     * @param bs the second list
     * @param <A> the element type of {@code as}
     * @param <B> the element type of {@code bs}
     * @return a new, mutable list of map entries pairing {@code as.get(i)} with
     *     {@code bs.get(i)} for {@code i} from {@code 0} up to the shorter list's length;
     *     entries permit {@code null} keys and values
     * @throws NullPointerException if {@code as} or {@code bs} is {@code null}
     */
    public static <A, B> List<Map.Entry<A, B>> zip(List<A> as, List<B> bs) {
        Validate.notNull(as, "as");
        Validate.notNull(bs, "bs");
        int length = Math.min(as.size(), bs.size());
        List<Map.Entry<A, B>> result = new ArrayList<>(length);
        for (int i = 0; i < length; i++) {
            result.add(new AbstractMap.SimpleImmutableEntry<>(as.get(i), bs.get(i)));
        }
        return result;
    }

    /**
     * Groups the elements of {@code source} by the key produced by {@code classifier}.
     *
     * @param source the elements to group
     * @param classifier computes the group key for each element
     * @param <T> the element type
     * @param <K> the key type
     * @return a new, mutable {@link LinkedHashMap} from each distinct key (in the order
     *     first produced) to a new, mutable list of the elements that produced it, in
     *     source order
     * @throws NullPointerException if {@code source} or {@code classifier} is
     *     {@code null}
     */
    public static <T, K> Map<K, List<T>> groupBy(Collection<T> source, Function<? super T, ? extends K> classifier) {
        Validate.notNull(source, PARAM_SOURCE);
        Validate.notNull(classifier, "classifier");
        Map<K, List<T>> result = new LinkedHashMap<>();
        for (T element : source) {
            K key = classifier.apply(element);
            result.computeIfAbsent(key, k -> new ArrayList<>()).add(element);
        }
        return result;
    }

    /**
     * Counts the elements of {@code source} for each key produced by {@code classifier}.
     *
     * @param source the elements to count
     * @param classifier computes the key for each element
     * @param <T> the element type
     * @param <K> the key type
     * @return a new, mutable {@link LinkedHashMap} from each distinct key (in the order
     *     first produced) to the number of elements that produced it
     * @throws NullPointerException if {@code source} or {@code classifier} is
     *     {@code null}
     */
    public static <T, K> Map<K, Long> countBy(Collection<T> source, Function<? super T, ? extends K> classifier) {
        Validate.notNull(source, PARAM_SOURCE);
        Validate.notNull(classifier, "classifier");
        Map<K, Long> result = new LinkedHashMap<>();
        for (T element : source) {
            K key = classifier.apply(element);
            result.merge(key, 1L, Long::sum);
        }
        return result;
    }

    /**
     * Sorts the elements of {@code source} according to {@code comparator}.
     *
     * @param source the elements to sort
     * @param comparator the ordering to apply
     * @param <T> the element type
     * @return a new, mutable list holding the elements of {@code source} in sorted
     *     order; {@code source} itself is left unmodified
     * @throws NullPointerException if {@code source} or {@code comparator} is
     *     {@code null}
     */
    public static <T> List<T> sortBy(Collection<T> source, Comparator<? super T> comparator) {
        Validate.notNull(source, PARAM_SOURCE);
        Validate.notNull(comparator, "comparator");
        List<T> result = new ArrayList<>(source);
        result.sort(comparator);
        return result;
    }

    /**
     * Finds the smallest element of {@code source} according to {@code comparator}.
     *
     * @param source the elements to scan
     * @param comparator the ordering to apply
     * @param <T> the element type
     * @return the smallest element, or {@link Optional#empty()} if {@code source} is
     *     empty or the smallest element is itself {@code null}
     * @throws NullPointerException if {@code source} or {@code comparator} is
     *     {@code null}
     */
    public static <T> Optional<T> minBy(Collection<T> source, Comparator<? super T> comparator) {
        Validate.notNull(source, PARAM_SOURCE);
        Validate.notNull(comparator, "comparator");
        return extreme(source, comparator, -1);
    }

    /**
     * Finds the largest element of {@code source} according to {@code comparator}.
     *
     * @param source the elements to scan
     * @param comparator the ordering to apply
     * @param <T> the element type
     * @return the largest element, or {@link Optional#empty()} if {@code source} is
     *     empty or the largest element is itself {@code null}
     * @throws NullPointerException if {@code source} or {@code comparator} is
     *     {@code null}
     */
    public static <T> Optional<T> maxBy(Collection<T> source, Comparator<? super T> comparator) {
        Validate.notNull(source, PARAM_SOURCE);
        Validate.notNull(comparator, "comparator");
        return extreme(source, comparator, 1);
    }

    /**
     * Shared scan used by {@link #minBy} and {@link #maxBy}.
     *
     * @param source the elements to scan, already validated non-null
     * @param comparator the ordering to apply, already validated non-null
     * @param wantedSign {@code -1} to keep the smaller element on ties/improvements
     *     (minimum), {@code 1} to keep the larger element (maximum)
     * @param <T> the element type
     * @return the selected extreme element, or {@link Optional#empty()} if empty or null
     */
    private static <T> Optional<T> extreme(Collection<T> source, Comparator<? super T> comparator, int wantedSign) {
        boolean found = false;
        T best = null;
        for (T element : source) {
            if (!found || Integer.signum(comparator.compare(element, best)) == wantedSign) {
                best = element;
                found = true;
            }
        }
        return Optional.ofNullable(best);
    }

    /**
     * Returns the first {@code n} elements of {@code source}.
     *
     * @param source the elements to take from
     * @param n the number of elements to take; must be {@code >= 0}
     * @param <T> the element type
     * @return a new, mutable list holding the first {@code n} elements of
     *     {@code source}, or all of {@code source} if it has fewer than {@code n}
     *     elements
     * @throws NullPointerException if {@code source} is {@code null}
     * @throws IllegalArgumentException if {@code n} is negative
     */
    public static <T> List<T> take(List<T> source, int n) {
        Validate.notNull(source, PARAM_SOURCE);
        if (n < 0) {
            throw new IllegalArgumentException("n must not be negative, was " + n);
        }
        return new ArrayList<>(source.subList(0, Math.min(n, source.size())));
    }

    /**
     * Returns all but the first {@code n} elements of {@code source}.
     *
     * @param source the elements to drop from
     * @param n the number of leading elements to drop; must be {@code >= 0}
     * @param <T> the element type
     * @return a new, mutable list holding {@code source} without its first {@code n}
     *     elements; empty if {@code n >= source.size()}
     * @throws NullPointerException if {@code source} is {@code null}
     * @throws IllegalArgumentException if {@code n} is negative
     */
    public static <T> List<T> drop(List<T> source, int n) {
        Validate.notNull(source, PARAM_SOURCE);
        if (n < 0) {
            throw new IllegalArgumentException("n must not be negative, was " + n);
        }
        return new ArrayList<>(source.subList(Math.min(n, source.size()), source.size()));
    }

    /**
     * Returns the first element of {@code source}, if any.
     *
     * @param source the list to read from
     * @param <T> the element type
     * @return the first element, or {@link Optional#empty()} if {@code source} is empty
     *     or its first element is itself {@code null}
     * @throws NullPointerException if {@code source} is {@code null}
     */
    public static <T> Optional<T> first(List<T> source) {
        Validate.notNull(source, PARAM_SOURCE);
        return source.isEmpty() ? Optional.empty() : Optional.ofNullable(source.get(0));
    }

    /**
     * Returns the last element of {@code source}, if any.
     *
     * @param source the list to read from
     * @param <T> the element type
     * @return the last element, or {@link Optional#empty()} if {@code source} is empty
     *     or its last element is itself {@code null}
     * @throws NullPointerException if {@code source} is {@code null}
     */
    public static <T> Optional<T> last(List<T> source) {
        Validate.notNull(source, PARAM_SOURCE);
        return source.isEmpty() ? Optional.empty() : Optional.ofNullable(source.get(source.size() - 1));
    }
}
