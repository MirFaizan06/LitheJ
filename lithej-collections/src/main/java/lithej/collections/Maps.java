package lithej.collections;

import java.util.ArrayList;
import java.util.Collection;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.BinaryOperator;
import java.util.function.Function;
import java.util.function.Predicate;
import lithej.core.Validate;

/**
 * Static helpers over {@link Map}: filtering, transforming, merging and inverting,
 * plus grouping a source collection into a multi-map.
 *
 * <p>This class deliberately does not provide a {@code Maps.of(...)} factory: the JDK's
 * {@link Map#of(Object, Object)} family (and {@link Map#ofEntries(Map.Entry[])} for
 * larger maps) already covers immutable-map construction with no real ergonomic gain
 * available from wrapping it again.
 *
 * <p><b>Null policy:</b> a {@code null} map/collection argument is always rejected with a
 * {@link NullPointerException} via {@link Validate#notNull}; it is never silently treated
 * as empty. A {@code null} function/predicate argument is rejected the same way.
 * Individual keys and values may be {@code null} unless a method says otherwise.
 *
 * <p>Returned maps are new, mutable {@link LinkedHashMap}s, preserving the iteration
 * order of the source map (or the order keys are first produced, for
 * {@link #groupBy(Collection, Function, Function)}), unless documented otherwise.
 *
 * <p><b>Thread safety:</b> this class is stateless and thread-safe.
 */
public final class Maps {

    private static final String PARAM_SOURCE = "source";

    private Maps() {
    }

    /**
     * Inverts {@code source}, swapping keys and values.
     *
     * <p>Because map values are not required to be unique, inverting a map can lose
     * data if two keys share the same value: only one of them could survive as a key in
     * the result. Rather than silently dropping one, this method fails loudly.
     *
     * @param source the map to invert
     * @param <K> the source key / result value type
     * @param <V> the source value / result key type
     * @return a new, mutable map from each value of {@code source} to the key that
     *     produced it
     * @throws NullPointerException if {@code source} is {@code null}
     * @throws IllegalStateException if two different keys in {@code source} map to the
     *     same value, since inverting would then silently discard one of the keys
     */
    public static <K, V> Map<V, K> invert(Map<K, V> source) {
        Validate.notNull(source, PARAM_SOURCE);
        Map<V, K> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : source.entrySet()) {
            V value = entry.getValue();
            if (result.containsKey(value)) {
                throw new IllegalStateException(
                        "cannot invert: value " + value + " is produced by multiple keys ("
                                + result.get(value) + " and " + entry.getKey() + ")");
            }
            result.put(value, entry.getKey());
        }
        return result;
    }

    /**
     * Keeps only the entries of {@code source} whose key satisfies {@code predicate}.
     *
     * @param source the map to filter
     * @param predicate the test applied to each key
     * @param <K> the key type
     * @param <V> the value type
     * @return a new, mutable map holding the entries of {@code source} whose key
     *     matched {@code predicate}
     * @throws NullPointerException if {@code source} or {@code predicate} is
     *     {@code null}
     */
    public static <K, V> Map<K, V> filterKeys(Map<K, V> source, Predicate<? super K> predicate) {
        Validate.notNull(source, PARAM_SOURCE);
        Validate.notNull(predicate, "predicate");
        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : source.entrySet()) {
            if (predicate.test(entry.getKey())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Keeps only the entries of {@code source} whose value satisfies {@code predicate}.
     *
     * @param source the map to filter
     * @param predicate the test applied to each value
     * @param <K> the key type
     * @param <V> the value type
     * @return a new, mutable map holding the entries of {@code source} whose value
     *     matched {@code predicate}
     * @throws NullPointerException if {@code source} or {@code predicate} is
     *     {@code null}
     */
    public static <K, V> Map<K, V> filterValues(Map<K, V> source, Predicate<? super V> predicate) {
        Validate.notNull(source, PARAM_SOURCE);
        Validate.notNull(predicate, "predicate");
        Map<K, V> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : source.entrySet()) {
            if (predicate.test(entry.getValue())) {
                result.put(entry.getKey(), entry.getValue());
            }
        }
        return result;
    }

    /**
     * Transforms every value of {@code source} with {@code mapper}, keeping the keys
     * unchanged.
     *
     * @param source the map to transform
     * @param mapper the transformation applied to each value
     * @param <K> the key type
     * @param <V> the source value type
     * @param <R> the result value type
     * @return a new, mutable map from each key of {@code source} to
     *     {@code mapper.apply(value)}
     * @throws NullPointerException if {@code source} or {@code mapper} is {@code null}
     */
    public static <K, V, R> Map<K, R> mapValues(Map<K, V> source, Function<? super V, ? extends R> mapper) {
        Validate.notNull(source, PARAM_SOURCE);
        Validate.notNull(mapper, "mapper");
        Map<K, R> result = new LinkedHashMap<>();
        for (Map.Entry<K, V> entry : source.entrySet()) {
            result.put(entry.getKey(), mapper.apply(entry.getValue()));
        }
        return result;
    }

    /**
     * Merges {@code first} and {@code second} into a single map, resolving keys present
     * in both with {@code conflictResolver}.
     *
     * <p>Every key unique to {@code first} or unique to {@code second} is carried over
     * unchanged. For a key present in both, {@code conflictResolver} is invoked as
     * {@code conflictResolver.apply(valueFromFirst, valueFromSecond)} and its result
     * becomes the value for that key. Pass {@code (a, b) -> b} to make {@code second}'s
     * values win outright on conflicts, or {@code (a, b) -> a} to make {@code first}'s
     * values win.
     *
     * <pre>{@code
     * Map<String, Integer> merged = Maps.merge(
     *         Map.of("a", 1, "b", 2),
     *         Map.of("b", 20, "c", 3),
     *         (fromFirst, fromSecond) -> fromSecond);
     * // {a=1, b=20, c=3}
     * }</pre>
     *
     * @param first the first map
     * @param second the second map
     * @param conflictResolver resolves the value for a key present in both maps
     * @param <K> the key type
     * @param <V> the value type
     * @return a new, mutable map containing every key of {@code first} and
     *     {@code second}, with conflicts resolved by {@code conflictResolver}
     * @throws NullPointerException if {@code first}, {@code second}, or
     *     {@code conflictResolver} is {@code null}
     */
    public static <K, V> Map<K, V> merge(Map<K, V> first, Map<K, V> second, BinaryOperator<V> conflictResolver) {
        Validate.notNull(first, "first");
        Validate.notNull(second, "second");
        Validate.notNull(conflictResolver, "conflictResolver");
        Map<K, V> result = new LinkedHashMap<>(first);
        for (Map.Entry<K, V> entry : second.entrySet()) {
            K key = entry.getKey();
            V newValue = entry.getValue();
            if (result.containsKey(key)) {
                result.put(key, conflictResolver.apply(result.get(key), newValue));
            } else {
                result.put(key, newValue);
            }
        }
        return result;
    }

    /**
     * Groups the elements of {@code source} into a multi-map, keyed by {@code keyFn} and
     * valued by {@code valueFn}.
     *
     * <pre>{@code
     * Map<Integer, List<String>> byLength =
     *         Maps.groupBy(List.of("a", "bb", "cc", "ddd"), String::length, Function.identity());
     * // {1=[a], 2=[bb, cc], 3=[ddd]}
     * }</pre>
     *
     * @param source the elements to group
     * @param keyFn computes the group key for each element
     * @param valueFn computes the stored value for each element
     * @param <T> the source element type
     * @param <K> the key type
     * @param <V> the value type
     * @return a new, mutable map from each distinct key (in the order first produced) to
     *     a new, mutable list of the values produced for it, in source order
     * @throws NullPointerException if {@code source}, {@code keyFn}, or {@code valueFn}
     *     is {@code null}
     */
    public static <T, K, V> Map<K, List<V>> groupBy(
            Collection<T> source, Function<? super T, ? extends K> keyFn, Function<? super T, ? extends V> valueFn) {
        Validate.notNull(source, PARAM_SOURCE);
        Validate.notNull(keyFn, "keyFn");
        Validate.notNull(valueFn, "valueFn");
        Map<K, List<V>> result = new LinkedHashMap<>();
        for (T element : source) {
            K key = keyFn.apply(element);
            result.computeIfAbsent(key, k -> new ArrayList<>()).add(valueFn.apply(element));
        }
        return result;
    }
}
