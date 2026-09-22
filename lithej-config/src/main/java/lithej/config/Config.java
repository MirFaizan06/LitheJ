package lithej.config;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Properties;
import java.util.function.Function;
import lithej.core.Numbers;
import lithej.core.Validate;

/**
 * A small, immutable, layered configuration source with explicit, caller-controlled
 * precedence.
 *
 * <p>A {@code Config} is built by chaining {@code withX(...)} calls, each adding one
 * more layer to consult. <b>Layers are consulted in the order they were added, and
 * the first layer that has a value for a key wins</b> — so list higher-priority
 * overrides first and fall back to lower-priority sources and defaults later:
 *
 * <pre>{@code
 * Config config = Config.empty()
 *         .withSystemProperties()      // -Dport=8080 on the java command line
 *         .withEnvironmentVariables()  // PORT=8080 in the shell
 *         .withProperties(fileProps)   // port=8080 in app.properties
 *         .withDefault("port", "8080");// fallback if nothing else set it
 *
 * int port = config.getInt("port", 8080);
 * }</pre>
 *
 * <p>Each {@code withX} method returns a new {@code Config}; the receiver is
 * unmodified. There is no reload/refresh mechanism: a {@code Config} is a snapshot of
 * its sources' state at the moment each layer was added (system properties and
 * environment variables are read lazily, at lookup time, since they rarely change
 * within a process's lifetime — but a {@link Properties} or {@link Map} layer you add
 * is captured as given and will reflect later mutations to that same object, since it
 * is not defensively copied).
 *
 * <p><b>Thread safety:</b> a {@code Config} instance is immutable and safe to share
 * across threads, provided any {@link Properties} or {@link Map} layers you added are
 * not concurrently mutated by other code.
 */
public final class Config {

    private static final Config EMPTY = new Config(List.of());

    private final List<Function<String, Optional<String>>> sources;

    private Config(List<Function<String, Optional<String>>> sources) {
        this.sources = sources;
    }

    /**
     * Returns a {@code Config} with no layers; every lookup returns
     * {@link Optional#empty()} until you add at least one layer or fallback default.
     *
     * @return an empty configuration
     */
    public static Config empty() {
        return EMPTY;
    }

    /**
     * Returns a copy of this configuration with JVM system properties
     * ({@link System#getProperty(String)}) added as the next layer to consult.
     *
     * @return a new {@code Config} with system properties added
     */
    public Config withSystemProperties() {
        return withSource(key -> Optional.ofNullable(System.getProperty(key)));
    }

    /**
     * Returns a copy of this configuration with process environment variables
     * ({@link Env#get(String)}) added as the next layer to consult.
     *
     * @return a new {@code Config} with environment variables added
     */
    public Config withEnvironmentVariables() {
        return withSource(Env::get);
    }

    /**
     * Returns a copy of this configuration with {@code properties} added as the next
     * layer to consult. {@code properties} is captured by reference, not copied.
     *
     * @param properties the properties to add as a layer
     * @return a new {@code Config} with {@code properties} added
     * @throws NullPointerException if {@code properties} is {@code null}
     */
    public Config withProperties(Properties properties) {
        Validate.notNull(properties, "properties");
        return withSource(key -> Optional.ofNullable(properties.getProperty(key)));
    }

    /**
     * Returns a copy of this configuration with {@code values} added as the next
     * layer to consult. {@code values} is captured by reference, not copied.
     *
     * @param values the map to add as a layer
     * @return a new {@code Config} with {@code values} added
     * @throws NullPointerException if {@code values} is {@code null}
     */
    public Config withMap(Map<String, String> values) {
        Validate.notNull(values, "values");
        return withSource(key -> Optional.ofNullable(values.get(key)));
    }

    /**
     * Returns a copy of this configuration with a single fixed key/value pair added
     * as the lowest-priority-so-far layer. Typically used last, as a final fallback.
     *
     * @param key the key this default applies to
     * @param value the fallback value
     * @return a new {@code Config} with the default added
     * @throws NullPointerException if either argument is {@code null}
     */
    public Config withDefault(String key, String value) {
        Validate.notNull(key, "key");
        Validate.notNull(value, "value");
        return withSource(k -> k.equals(key) ? Optional.of(value) : Optional.empty());
    }

    /**
     * Returns the value of {@code key} from the first layer that has it.
     *
     * @param key the configuration key
     * @return the value from the highest-priority layer that has {@code key}, or
     *     {@link Optional#empty()} if no layer has it
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public Optional<String> get(String key) {
        Validate.notNull(key, "key");
        for (Function<String, Optional<String>> source : sources) {
            Optional<String> value = source.apply(key);
            if (value.isPresent()) {
                return value;
            }
        }
        return Optional.empty();
    }

    /**
     * Returns the value of {@code key}.
     *
     * @param key the configuration key
     * @return the value from the highest-priority layer that has {@code key}
     * @throws NullPointerException if {@code key} is {@code null}
     * @throws NoSuchElementException if no layer has {@code key}
     */
    public String require(String key) {
        return get(key).orElseThrow(() -> new NoSuchElementException("Required configuration key is not set: " + key));
    }

    /**
     * Returns the value of {@code key}, or {@code defaultValue} if no layer has it.
     *
     * @param key the configuration key
     * @param defaultValue the value to return if no layer has {@code key}
     * @return the value, or {@code defaultValue}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public String getOrDefault(String key, String defaultValue) {
        return get(key).orElse(defaultValue);
    }

    /**
     * Returns the value of {@code key} parsed as an {@code int}, or
     * {@code defaultValue} if no layer has it or its value is not a valid
     * {@code int}.
     *
     * @param key the configuration key
     * @param defaultValue the value to return if the key is missing or invalid
     * @return the parsed value, or {@code defaultValue}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public int getInt(String key, int defaultValue) {
        Optional<String> value = get(key);
        if (value.isEmpty()) {
            return defaultValue;
        }
        OptionalInt parsed = Numbers.tryInt(value.get());
        return parsed.isPresent() ? parsed.getAsInt() : defaultValue;
    }

    /**
     * Returns the value of {@code key} parsed as a boolean via
     * {@link Boolean#parseBoolean(String)} (case-insensitive {@code "true"}, anything
     * else is {@code false}), or {@code defaultValue} if no layer has it.
     *
     * @param key the configuration key
     * @param defaultValue the value to return if no layer has {@code key}
     * @return the parsed value, or {@code defaultValue}
     * @throws NullPointerException if {@code key} is {@code null}
     */
    public boolean getBoolean(String key, boolean defaultValue) {
        return get(key).map(Boolean::parseBoolean).orElse(defaultValue);
    }

    private Config withSource(Function<String, Optional<String>> source) {
        List<Function<String, Optional<String>>> combined = new ArrayList<>(sources);
        combined.add(source);
        return new Config(List.copyOf(combined));
    }
}
