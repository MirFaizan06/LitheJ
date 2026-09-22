package lithej.core;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;

/**
 * Parsing, clamping and range operations for numeric values, built on top of
 * {@link Integer}, {@link Long} and {@link Double}.
 *
 * <p>Two parsing families are provided: {@code parseX} methods behave exactly like the
 * corresponding JDK {@code parseX} method (throwing {@link NumberFormatException} on
 * invalid input) but trim surrounding whitespace first; {@code tryX} methods never
 * throw, returning an empty {@code OptionalX} instead.
 *
 * <p><b>Thread safety:</b> this class is stateless and thread-safe.
 */
public final class Numbers {

    private Numbers() {
    }

    /**
     * Parses {@code value} as an {@code int}, trimming surrounding whitespace first.
     *
     * @param value the string to parse
     * @return the parsed value
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws NumberFormatException if {@code value} (after trimming) is not a valid
     *     {@code int}, or overflows the {@code int} range
     */
    public static int parseInt(String value) {
        return Integer.parseInt(value.trim());
    }

    /**
     * Parses {@code value} as an {@code int}, trimming surrounding whitespace first.
     *
     * @param value the string to parse
     * @return an {@link OptionalInt} holding the parsed value, or empty if {@code value}
     *     is {@code null}, blank, or not a valid {@code int}
     */
    public static OptionalInt tryInt(String value) {
        if (value == null || value.isBlank()) {
            return OptionalInt.empty();
        }
        try {
            return OptionalInt.of(Integer.parseInt(value.trim()));
        } catch (NumberFormatException e) {
            return OptionalInt.empty();
        }
    }

    /**
     * Parses {@code value} as a {@code long}, trimming surrounding whitespace first.
     *
     * @param value the string to parse
     * @return the parsed value
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws NumberFormatException if {@code value} (after trimming) is not a valid
     *     {@code long}, or overflows the {@code long} range
     */
    public static long parseLong(String value) {
        return Long.parseLong(value.trim());
    }

    /**
     * Parses {@code value} as a {@code long}, trimming surrounding whitespace first.
     *
     * @param value the string to parse
     * @return an {@link OptionalLong} holding the parsed value, or empty if
     *     {@code value} is {@code null}, blank, or not a valid {@code long}
     */
    public static OptionalLong tryLong(String value) {
        if (value == null || value.isBlank()) {
            return OptionalLong.empty();
        }
        try {
            return OptionalLong.of(Long.parseLong(value.trim()));
        } catch (NumberFormatException e) {
            return OptionalLong.empty();
        }
    }

    /**
     * Parses {@code value} as a {@code double}, trimming surrounding whitespace first.
     *
     * @param value the string to parse
     * @return the parsed value
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws NumberFormatException if {@code value} (after trimming) is not a valid
     *     {@code double}
     */
    public static double parseDouble(String value) {
        return Double.parseDouble(value.trim());
    }

    /**
     * Parses {@code value} as a {@code double}, trimming surrounding whitespace first.
     *
     * @param value the string to parse
     * @return an {@link OptionalDouble} holding the parsed value, or empty if
     *     {@code value} is {@code null}, blank, or not a valid {@code double}
     */
    public static OptionalDouble tryDouble(String value) {
        if (value == null || value.isBlank()) {
            return OptionalDouble.empty();
        }
        try {
            return OptionalDouble.of(Double.parseDouble(value.trim()));
        } catch (NumberFormatException e) {
            return OptionalDouble.empty();
        }
    }

    /**
     * Returns {@code true} if {@code value} can be parsed as an {@code int} by
     * {@link #parseInt(String)}.
     *
     * @param value the string to check, may be {@code null}
     * @return {@code true} if {@code value} is a valid {@code int}
     */
    public static boolean isInteger(String value) {
        return tryInt(value).isPresent();
    }

    /**
     * Constrains {@code value} to the inclusive range {@code [min, max]}.
     *
     * @param value the value to clamp
     * @param min the inclusive lower bound
     * @param max the inclusive upper bound
     * @return {@code min} if {@code value < min}, {@code max} if {@code value > max},
     *     otherwise {@code value}
     * @throws IllegalArgumentException if {@code min > max}
     */
    public static int clamp(int value, int min, int max) {
        requireOrderedBounds(min, max);
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Constrains {@code value} to the inclusive range {@code [min, max]}.
     *
     * @param value the value to clamp
     * @param min the inclusive lower bound
     * @param max the inclusive upper bound
     * @return {@code min} if {@code value < min}, {@code max} if {@code value > max},
     *     otherwise {@code value}
     * @throws IllegalArgumentException if {@code min > max}
     */
    public static long clamp(long value, long min, long max) {
        requireOrderedBounds(min, max);
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Constrains {@code value} to the inclusive range {@code [min, max]}.
     *
     * @param value the value to clamp
     * @param min the inclusive lower bound
     * @param max the inclusive upper bound
     * @return {@code min} if {@code value < min}, {@code max} if {@code value > max},
     *     otherwise {@code value}
     * @throws IllegalArgumentException if {@code min > max}
     */
    public static double clamp(double value, double min, double max) {
        requireOrderedBounds(min, max);
        return Math.max(min, Math.min(max, value));
    }

    /**
     * Returns {@code true} if {@code value} falls within {@code [min, max]} inclusive.
     *
     * @param value the value to check
     * @param min the inclusive lower bound
     * @param max the inclusive upper bound
     * @return {@code true} if {@code min <= value <= max}
     */
    public static boolean inRange(int value, int min, int max) {
        return value >= min && value <= max;
    }

    /**
     * Returns {@code true} if {@code value} falls within {@code [min, max]} inclusive.
     *
     * @param value the value to check
     * @param min the inclusive lower bound
     * @param max the inclusive upper bound
     * @return {@code true} if {@code min <= value <= max}
     */
    public static boolean inRange(long value, long min, long max) {
        return value >= min && value <= max;
    }

    /**
     * Returns {@code true} if {@code value} falls within {@code [min, max]} inclusive.
     *
     * @param value the value to check
     * @param min the inclusive lower bound
     * @param max the inclusive upper bound
     * @return {@code true} if {@code min <= value <= max}
     */
    public static boolean inRange(double value, double min, double max) {
        return value >= min && value <= max;
    }

    /**
     * Rounds {@code value} to {@code decimalPlaces} digits after the decimal point,
     * using {@link RoundingMode#HALF_UP}.
     *
     * <p>This uses {@link BigDecimal} internally to avoid the binary-floating-point
     * surprises of naive {@code Math.round(value * 100) / 100.0} approaches (e.g.
     * {@code 2.675} rounding down to {@code 2.67} under plain {@code double} math).
     *
     * @param value the value to round; must be finite
     * @param decimalPlaces the number of digits to keep after the decimal point; must be
     *     {@code >= 0}
     * @return {@code value} rounded to {@code decimalPlaces} digits
     * @throws IllegalArgumentException if {@code value} is {@code NaN} or infinite, or if
     *     {@code decimalPlaces} is negative
     */
    public static double round(double value, int decimalPlaces) {
        if (Double.isNaN(value) || Double.isInfinite(value)) {
            throw new IllegalArgumentException("value must be finite, was " + value);
        }
        if (decimalPlaces < 0) {
            throw new IllegalArgumentException("decimalPlaces must not be negative, was " + decimalPlaces);
        }
        return BigDecimal.valueOf(value).setScale(decimalPlaces, RoundingMode.HALF_UP).doubleValue();
    }

    private static void requireOrderedBounds(long min, long max) {
        if (min > max) {
            throw new IllegalArgumentException("min (" + min + ") must not exceed max (" + max + ")");
        }
    }

    private static void requireOrderedBounds(double min, double max) {
        if (min > max) {
            throw new IllegalArgumentException("min (" + min + ") must not exceed max (" + max + ")");
        }
    }
}
