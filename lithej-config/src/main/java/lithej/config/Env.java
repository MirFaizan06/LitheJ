package lithej.config;

import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.OptionalLong;
import java.util.Set;
import lithej.core.Numbers;
import lithej.core.Validate;

/**
 * Read access to process environment variables ({@link System#getenv()}), with typed
 * parsing helpers.
 *
 * <p>Environment variables are set once when the JVM starts and cannot be changed at
 * runtime, so this class is read-only: there is no {@code Env.set(...)}. For values
 * your own application controls and may want to change or layer with other sources,
 * use {@link Config} instead.
 *
 * <p><b>Thread safety:</b> this class is stateless and thread-safe;
 * {@link System#getenv()} itself returns an immutable, thread-safe map.
 */
public final class Env {

    private static final Set<String> TRUE_VALUES = Set.of("true", "1", "yes", "y");
    private static final Set<String> FALSE_VALUES = Set.of("false", "0", "no", "n");

    private Env() {
    }

    /**
     * Returns the value of environment variable {@code name}, if set.
     *
     * @param name the environment variable name
     * @return the variable's value, or {@link Optional#empty()} if it is not set
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public static Optional<String> get(String name) {
        Validate.notNull(name, "name");
        return Optional.ofNullable(System.getenv(name));
    }

    /**
     * Returns the value of environment variable {@code name}, or {@code defaultValue}
     * if it is not set.
     *
     * @param name the environment variable name
     * @param defaultValue the value to return if {@code name} is not set
     * @return the variable's value, or {@code defaultValue}
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public static String getOrDefault(String name, String defaultValue) {
        return get(name).orElse(defaultValue);
    }

    /**
     * Returns the value of environment variable {@code name}.
     *
     * @param name the environment variable name
     * @return the variable's value
     * @throws NullPointerException if {@code name} is {@code null}
     * @throws NoSuchElementException if the variable is not set
     */
    public static String require(String name) {
        return get(name).orElseThrow(
                () -> new NoSuchElementException("Required environment variable is not set: " + name));
    }

    /**
     * Returns environment variable {@code name} parsed as an {@code int}.
     *
     * @param name the environment variable name
     * @return the parsed value, or {@link OptionalInt#empty()} if the variable is not
     *     set or is not a valid {@code int}
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public static OptionalInt getInt(String name) {
        Optional<String> value = get(name);
        return value.isPresent() ? Numbers.tryInt(value.get()) : OptionalInt.empty();
    }

    /**
     * Returns environment variable {@code name} parsed as an {@code int}, or
     * {@code defaultValue} if it is not set or is not a valid {@code int}.
     *
     * @param name the environment variable name
     * @param defaultValue the value to return if the variable is missing or invalid
     * @return the parsed value, or {@code defaultValue}
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public static int getInt(String name, int defaultValue) {
        OptionalInt value = getInt(name);
        return value.isPresent() ? value.getAsInt() : defaultValue;
    }

    /**
     * Returns environment variable {@code name} parsed as a {@code long}.
     *
     * @param name the environment variable name
     * @return the parsed value, or {@link OptionalLong#empty()} if the variable is not
     *     set or is not a valid {@code long}
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public static OptionalLong getLong(String name) {
        Optional<String> value = get(name);
        return value.isPresent() ? Numbers.tryLong(value.get()) : OptionalLong.empty();
    }

    /**
     * Returns environment variable {@code name} parsed as a boolean. Recognizes
     * (case-insensitively) {@code true}/{@code 1}/{@code yes}/{@code y} as
     * {@code true} and {@code false}/{@code 0}/{@code no}/{@code n} as {@code false}.
     *
     * @param name the environment variable name
     * @return the parsed value, or {@link Optional#empty()} if the variable is not
     *     set or its value is not one of the recognized forms
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public static Optional<Boolean> getBoolean(String name) {
        return get(name).flatMap(Env::parseBoolean);
    }

    /**
     * Returns environment variable {@code name} parsed as a boolean (see
     * {@link #getBoolean(String)} for recognized values), or {@code defaultValue} if
     * it is not set or not recognized.
     *
     * @param name the environment variable name
     * @param defaultValue the value to return if the variable is missing or
     *     unrecognized
     * @return the parsed value, or {@code defaultValue}
     * @throws NullPointerException if {@code name} is {@code null}
     */
    public static boolean getBoolean(String name, boolean defaultValue) {
        return getBoolean(name).orElse(defaultValue);
    }

    // Package-private (not private) so EnvTest can exercise every branch directly,
    // without needing to control real process environment variable content.
    static Optional<Boolean> parseBoolean(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        if (TRUE_VALUES.contains(normalized)) {
            return Optional.of(Boolean.TRUE);
        }
        if (FALSE_VALUES.contains(normalized)) {
            return Optional.of(Boolean.FALSE);
        }
        return Optional.empty();
    }
}
