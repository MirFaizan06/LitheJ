package lithej.collections;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.Set;
import lithej.core.Validate;

/**
 * Static helpers over {@link Set}: construction and the basic algebra of union,
 * intersection, difference and subset testing.
 *
 * <p><b>Null policy:</b> a {@code null} set argument is always rejected with a
 * {@link NullPointerException} via {@link Validate#notNull}; it is never silently
 * treated as empty.
 *
 * <p>Returned sets are new, mutable {@link LinkedHashSet}s (preserving encounter order)
 * unless documented otherwise; {@link #of} is the one exception, returning an immutable
 * set akin to {@link Set#of}.
 *
 * <p><b>Thread safety:</b> this class is stateless and thread-safe.
 */
public final class Sets {

    private Sets() {
    }

    /**
     * Creates an immutable set containing {@code items}, mirroring {@link Set#of(Object[])}.
     *
     * @param items the elements to include; must not contain duplicates or {@code null}
     * @param <T> the element type
     * @return a new, immutable set containing {@code items}
     * @throws NullPointerException if {@code items} (the array) or any element of it is
     *     {@code null}
     * @throws IllegalArgumentException if {@code items} contains a duplicate element
     */
    @SafeVarargs
    public static <T> Set<T> of(T... items) {
        Validate.notNull(items, "items");
        return Set.of(items);
    }

    /**
     * Creates a new mutable set containing the distinct elements of {@code items}, in
     * first-occurrence order.
     *
     * <p>Unlike {@link #of}, duplicate and {@code null} elements are permitted, matching
     * ordinary {@link LinkedHashSet} semantics (a duplicate is silently ignored, keeping
     * only its first occurrence's position).
     *
     * @param items the elements to include, may contain duplicates or {@code null}
     * @param <T> the element type
     * @return a new, mutable {@link LinkedHashSet} containing the distinct elements of
     *     {@code items}
     * @throws NullPointerException if {@code items} (the array) is {@code null}
     */
    @SafeVarargs
    public static <T> Set<T> mutable(T... items) {
        Validate.notNull(items, "items");
        return new LinkedHashSet<>(Arrays.asList(items));
    }

    /**
     * Computes the union of {@code a} and {@code b}: every element that appears in
     * either set.
     *
     * @param a the first set
     * @param b the second set
     * @param <T> the element type
     * @return a new, mutable set containing every element of {@code a} and {@code b},
     *     with {@code a}'s elements first
     * @throws NullPointerException if {@code a} or {@code b} is {@code null}
     */
    public static <T> Set<T> union(Set<T> a, Set<T> b) {
        Validate.notNull(a, "a");
        Validate.notNull(b, "b");
        Set<T> result = new LinkedHashSet<>(a);
        result.addAll(b);
        return result;
    }

    /**
     * Computes the intersection of {@code a} and {@code b}: every element that appears
     * in both sets.
     *
     * @param a the first set
     * @param b the second set
     * @param <T> the element type
     * @return a new, mutable set containing every element present in both {@code a} and
     *     {@code b}, in {@code a}'s iteration order
     * @throws NullPointerException if {@code a} or {@code b} is {@code null}
     */
    public static <T> Set<T> intersection(Set<T> a, Set<T> b) {
        Validate.notNull(a, "a");
        Validate.notNull(b, "b");
        Set<T> result = new LinkedHashSet<>(a);
        result.retainAll(b);
        return result;
    }

    /**
     * Computes the difference of {@code a} and {@code b}: the elements of {@code a} that
     * do not appear in {@code b}.
     *
     * @param a the set to subtract from
     * @param b the set of elements to remove
     * @param <T> the element type
     * @return a new, mutable set containing the elements of {@code a} that are not in
     *     {@code b}, in {@code a}'s iteration order
     * @throws NullPointerException if {@code a} or {@code b} is {@code null}
     */
    public static <T> Set<T> difference(Set<T> a, Set<T> b) {
        Validate.notNull(a, "a");
        Validate.notNull(b, "b");
        Set<T> result = new LinkedHashSet<>(a);
        result.removeAll(b);
        return result;
    }

    /**
     * Returns {@code true} if every element of {@code sub} is also present in
     * {@code superset}.
     *
     * <p>An empty {@code sub} is a subset of any set, including an empty one.
     *
     * @param sub the candidate subset
     * @param superset the candidate superset
     * @param <T> the element type
     * @return {@code true} if {@code superset} contains every element of {@code sub}
     * @throws NullPointerException if {@code sub} or {@code superset} is {@code null}
     */
    public static <T> boolean isSubsetOf(Set<T> sub, Set<T> superset) {
        Validate.notNull(sub, "sub");
        Validate.notNull(superset, "superset");
        return superset.containsAll(sub);
    }
}
