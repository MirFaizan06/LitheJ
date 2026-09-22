package lithej.core;

import java.util.Collection;
import java.util.Map;
import java.util.regex.Pattern;

/**
 * Argument and state validation helpers that throw a standard JDK exception with a
 * clear, consistent message instead of requiring hand-written {@code if}/{@code throw}
 * blocks at every call site.
 *
 * <p>Every method returns the validated value (or {@code true}/{@code false} for the
 * boolean predicates), so checks can be inlined at the point of use:
 *
 * <pre>{@code
 * this.name = Validate.notBlank(name, "name");
 * this.age = Validate.range(age, 0, 150, "age");
 * }</pre>
 *
 * <p><b>Exception policy:</b> methods that check nullability throw
 * {@link NullPointerException}; methods that check a value's content or shape throw
 * {@link IllegalArgumentException}. This split matches the convention used by the JDK
 * itself (e.g. {@link java.util.Objects#requireNonNull}) so exception-type-based
 * {@code catch} blocks behave predictably.
 *
 * <p><b>Thread safety:</b> this class is stateless and thread-safe.
 */
public final class Validate {

    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$");

    private Validate() {
    }

    /**
     * Requires {@code value} to be non-null.
     *
     * @param value the value to check
     * @param name the parameter name to include in the exception message, e.g.
     *     {@code "userId"}
     * @param <T> the value type
     * @return {@code value}, never {@code null}
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public static <T> T notNull(T value, String name) {
        if (value == null) {
            throw new NullPointerException(name + " must not be null");
        }
        return value;
    }

    /**
     * Requires {@code value} to be non-null and non-blank (not empty and not all
     * whitespace).
     *
     * @param value the string to check, may be {@code null}
     * @param name the parameter name to include in the exception message
     * @return {@code value}, guaranteed non-null and non-blank
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} is empty or all whitespace
     */
    public static String notBlank(String value, String name) {
        notNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }

    /**
     * Requires {@code collection} to be non-null and non-empty.
     *
     * @param collection the collection to check, may be {@code null}
     * @param name the parameter name to include in the exception message
     * @param <T> the collection type
     * @return {@code collection}, guaranteed non-null and non-empty
     * @throws NullPointerException if {@code collection} is {@code null}
     * @throws IllegalArgumentException if {@code collection} is empty
     */
    public static <T extends Collection<?>> T notEmpty(T collection, String name) {
        notNull(collection, name);
        if (collection.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return collection;
    }

    /**
     * Requires {@code map} to be non-null and non-empty.
     *
     * @param map the map to check, may be {@code null}
     * @param name the parameter name to include in the exception message
     * @param <T> the map type
     * @return {@code map}, guaranteed non-null and non-empty
     * @throws NullPointerException if {@code map} is {@code null}
     * @throws IllegalArgumentException if {@code map} is empty
     */
    public static <T extends Map<?, ?>> T notEmpty(T map, String name) {
        notNull(map, name);
        if (map.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be empty");
        }
        return map;
    }

    /**
     * Requires {@code condition} to be {@code true}.
     *
     * @param condition the condition to check
     * @param message the exception message to use when {@code condition} is
     *     {@code false}
     * @throws IllegalArgumentException if {@code condition} is {@code false}
     */
    public static void isTrue(boolean condition, String message) {
        if (!condition) {
            throw new IllegalArgumentException(message);
        }
    }

    /**
     * Requires {@code value} to fall within {@code [min, max]} inclusive.
     *
     * @param value the value to check
     * @param min the inclusive lower bound
     * @param max the inclusive upper bound
     * @param name the parameter name to include in the exception message
     * @return {@code value}
     * @throws IllegalArgumentException if {@code value < min || value > max}, or if
     *     {@code min > max}
     */
    public static long range(long value, long min, long max, String name) {
        if (min > max) {
            throw new IllegalArgumentException("min (" + min + ") must not exceed max (" + max + ")");
        }
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    name + " must be between " + min + " and " + max + " (inclusive), was " + value);
        }
        return value;
    }

    /**
     * Requires {@code value} to fall within {@code [min, max]} inclusive.
     *
     * @param value the value to check
     * @param min the inclusive lower bound
     * @param max the inclusive upper bound
     * @param name the parameter name to include in the exception message
     * @return {@code value}
     * @throws IllegalArgumentException if {@code value < min || value > max}, or if
     *     {@code min > max}
     */
    public static double range(double value, double min, double max, String name) {
        if (min > max) {
            throw new IllegalArgumentException("min (" + min + ") must not exceed max (" + max + ")");
        }
        if (value < min || value > max) {
            throw new IllegalArgumentException(
                    name + " must be between " + min + " and " + max + " (inclusive), was " + value);
        }
        return value;
    }

    /**
     * Requires {@code value} to be strictly positive ({@code > 0}).
     *
     * @param value the value to check
     * @param name the parameter name to include in the exception message
     * @return {@code value}
     * @throws IllegalArgumentException if {@code value <= 0}
     */
    public static long positive(long value, String name) {
        if (value <= 0) {
            throw new IllegalArgumentException(name + " must be positive, was " + value);
        }
        return value;
    }

    /**
     * Requires {@code value} to be non-negative ({@code >= 0}).
     *
     * @param value the value to check
     * @param name the parameter name to include in the exception message
     * @return {@code value}
     * @throws IllegalArgumentException if {@code value < 0}
     */
    public static long nonNegative(long value, String name) {
        if (value < 0) {
            throw new IllegalArgumentException(name + " must not be negative, was " + value);
        }
        return value;
    }

    /**
     * Requires {@code value} to match {@code pattern} in its entirety
     * ({@link Pattern#matcher(CharSequence)} + {@code matches()}).
     *
     * @param value the string to check, may be {@code null}
     * @param pattern the pattern {@code value} must match
     * @param name the parameter name to include in the exception message
     * @return {@code value}
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code value} does not match {@code pattern}
     */
    public static String matches(String value, Pattern pattern, String name) {
        notNull(value, name);
        if (!pattern.matcher(value).matches()) {
            throw new IllegalArgumentException(name + " does not match the required pattern");
        }
        return value;
    }

    /**
     * Returns {@code true} if {@code value} has the basic syntactic shape of an email
     * address ({@code local@domain.tld}).
     *
     * <p>This is a syntax check only: it does not verify that the mailbox exists, that
     * the domain has valid DNS records, or that the address can actually receive mail.
     * Do not rely on it as proof of deliverability.
     *
     * @param value the string to check, may be {@code null}
     * @return {@code true} if {@code value} is non-null and looks like an email address
     */
    public static boolean isEmail(String value) {
        return value != null && EMAIL_PATTERN.matcher(value).matches();
    }
}
