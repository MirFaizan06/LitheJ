package lithej.core;

import java.util.function.Supplier;

/**
 * Null-safe convenience operations that complement {@link java.util.Objects}.
 *
 * <p>These methods never throw for a {@code null} input unless explicitly documented;
 * they exist to replace repetitive {@code value != null ? value : fallback} idioms with
 * a readable, intention-revealing call.
 *
 * <p><b>Thread safety:</b> this class is stateless and thread-safe. All methods are
 * pure functions of their arguments.
 */
public final class ObjectsX {

    private ObjectsX() {
    }

    /**
     * Returns {@code value} if it is not {@code null}, otherwise returns {@code fallback}.
     *
     * @param value the preferred value, may be {@code null}
     * @param fallback the value to use when {@code value} is {@code null}; may itself be
     *     {@code null}
     * @param <T> the value type
     * @return {@code value}, or {@code fallback} if {@code value} is {@code null}
     */
    public static <T> T orElse(T value, T fallback) {
        return value != null ? value : fallback;
    }

    /**
     * Returns {@code value} if it is not {@code null}, otherwise invokes {@code fallback}
     * and returns its result.
     *
     * <p>Use this overload instead of {@link #orElse(Object, Object)} when computing the
     * fallback is expensive, since {@code fallback} is only invoked when needed.
     *
     * @param value the preferred value, may be {@code null}
     * @param fallback supplier invoked to compute a fallback when {@code value} is
     *     {@code null}; must not be {@code null}
     * @param <T> the value type
     * @return {@code value}, or the result of {@code fallback.get()} if {@code value} is
     *     {@code null}
     * @throws NullPointerException if {@code value} is {@code null} and {@code fallback}
     *     is {@code null}
     */
    public static <T> T orElseGet(T value, Supplier<? extends T> fallback) {
        if (value != null) {
            return value;
        }
        return fallback.get();
    }

    /**
     * Returns the first argument that is not {@code null}, or {@code null} if every
     * argument is {@code null} (including when {@code values} itself is empty).
     *
     * @param values candidate values, evaluated left to right
     * @param <T> the value type
     * @return the first non-null value, or {@code null}
     */
    @SafeVarargs
    public static <T> T firstNonNull(T... values) {
        for (T value : values) {
            if (value != null) {
                return value;
            }
        }
        return null;
    }

    /**
     * Returns {@code true} if {@code value} is {@code null}.
     *
     * @param value the value to check
     * @return {@code true} if {@code value} is {@code null}
     */
    public static boolean isNull(Object value) {
        return value == null;
    }

    /**
     * Returns {@code true} if {@code value} is not {@code null}.
     *
     * @param value the value to check
     * @return {@code true} if {@code value} is not {@code null}
     */
    public static boolean isNotNull(Object value) {
        return value != null;
    }

    /**
     * Returns {@code String.valueOf(value)}, or {@code defaultValue} if {@code value} is
     * {@code null}.
     *
     * @param value the value to render, may be {@code null}
     * @param defaultValue the text to return when {@code value} is {@code null}; must not
     *     be {@code null}
     * @return the string representation of {@code value}, or {@code defaultValue}
     */
    public static String toStringOrDefault(Object value, String defaultValue) {
        return value != null ? String.valueOf(value) : defaultValue;
    }
}
