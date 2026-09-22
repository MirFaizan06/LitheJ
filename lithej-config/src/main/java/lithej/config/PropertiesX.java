package lithej.config;

import java.io.IOException;
import java.io.StringReader;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Properties;
import lithej.core.Numbers;
import lithej.core.Validate;
import lithej.io.FileIO;
import lithej.io.Resources;

/**
 * Loading and typed access helpers for {@link java.util.Properties}.
 *
 * <p><b>Encoding:</b> {@link Properties#load(java.io.Reader)} itself is
 * encoding-agnostic (it decodes whatever {@link java.io.Reader} you give it); the
 * traditional {@code .properties} convention is ISO-8859-1 with {@code \\uXXXX}
 * escapes for anything else, but this class defaults to UTF-8, matching the rest of
 * this library and modern practice (a UTF-8 {@code .properties} file works correctly
 * with editors and tooling that don't know about the legacy Latin-1 convention). Use
 * the {@link Charset}-accepting overload if you need the traditional encoding or
 * another one.
 *
 * <p><b>Thread safety:</b> this class is stateless and thread-safe. The
 * {@link Properties} instances it loads and reads are plain {@code java.util}
 * objects and are only as thread-safe as {@link java.util.Hashtable} (their
 * superclass) makes them: safe for concurrent reads, not for concurrent
 * read/write without external synchronization.
 */
public final class PropertiesX {

    private static final String PARAM_PROPERTIES = "properties";
    private static final String PARAM_KEY = "key";

    private PropertiesX() {
    }

    /**
     * Loads a {@code .properties} file as UTF-8.
     *
     * @param path the file to load
     * @return the loaded properties
     * @throws NullPointerException if {@code path} is {@code null}
     * @throws UncheckedIOException if {@code path} does not exist or reading fails
     */
    public static Properties load(Path path) {
        return load(path, StandardCharsets.UTF_8);
    }

    /**
     * Loads a {@code .properties} file, decoded using {@code charset}.
     *
     * @param path the file to load
     * @param charset the charset to decode with
     * @return the loaded properties
     * @throws NullPointerException if either argument is {@code null}
     * @throws UncheckedIOException if {@code path} does not exist or reading fails
     */
    public static Properties load(Path path, Charset charset) {
        Validate.notNull(path, "path");
        Validate.notNull(charset, "charset");
        String text = FileIO.read(path, charset);
        return parse(text);
    }

    /**
     * Loads a {@code .properties} file from the classpath, as UTF-8.
     *
     * @param resourcePath the classpath-relative resource path, no leading {@code /}
     * @return the loaded properties
     * @throws NullPointerException if {@code resourcePath} is {@code null}
     * @throws lithej.io.ResourceNotFoundException if no resource exists at
     *     {@code resourcePath}
     * @throws UncheckedIOException if reading fails for any other reason
     */
    public static Properties loadFromResource(String resourcePath) {
        Validate.notNull(resourcePath, "resourcePath");
        return parse(Resources.read(resourcePath));
    }

    /**
     * Returns the value of {@code key} in {@code properties}.
     *
     * @param properties the properties to read
     * @param key the property key
     * @return the property's value
     * @throws NullPointerException if either argument is {@code null}
     * @throws NoSuchElementException if {@code key} is not set
     */
    public static String require(Properties properties, String key) {
        return getString(properties, key).orElseThrow(
                () -> new NoSuchElementException("Required property is not set: " + key));
    }

    /**
     * Returns the value of {@code key} in {@code properties}, if set.
     *
     * @param properties the properties to read
     * @param key the property key
     * @return the property's value, or {@link Optional#empty()} if not set
     * @throws NullPointerException if either argument is {@code null}
     */
    public static Optional<String> getString(Properties properties, String key) {
        Validate.notNull(properties, PARAM_PROPERTIES);
        Validate.notNull(key, PARAM_KEY);
        return Optional.ofNullable(properties.getProperty(key));
    }

    /**
     * Returns the value of {@code key} in {@code properties}, or {@code defaultValue}
     * if it is not set.
     *
     * @param properties the properties to read
     * @param key the property key
     * @param defaultValue the value to return if {@code key} is not set
     * @return the property's value, or {@code defaultValue}
     * @throws NullPointerException if {@code properties} or {@code key} is
     *     {@code null}
     */
    public static String getString(Properties properties, String key, String defaultValue) {
        return getString(properties, key).orElse(defaultValue);
    }

    /**
     * Returns the value of {@code key} in {@code properties} parsed as an
     * {@code int}.
     *
     * @param properties the properties to read
     * @param key the property key
     * @return the parsed value, or {@link OptionalInt#empty()} if {@code key} is not
     *     set or is not a valid {@code int}
     * @throws NullPointerException if either argument is {@code null}
     */
    public static OptionalInt getInt(Properties properties, String key) {
        Optional<String> value = getString(properties, key);
        return value.isPresent() ? Numbers.tryInt(value.get()) : OptionalInt.empty();
    }

    /**
     * Returns the value of {@code key} in {@code properties} parsed as an
     * {@code int}, or {@code defaultValue} if it is not set or is not a valid
     * {@code int}.
     *
     * @param properties the properties to read
     * @param key the property key
     * @param defaultValue the value to return if the key is missing or invalid
     * @return the parsed value, or {@code defaultValue}
     * @throws NullPointerException if {@code properties} or {@code key} is
     *     {@code null}
     */
    public static int getInt(Properties properties, String key, int defaultValue) {
        OptionalInt value = getInt(properties, key);
        return value.isPresent() ? value.getAsInt() : defaultValue;
    }

    /**
     * Returns the value of {@code key} in {@code properties} parsed as a boolean via
     * {@link Boolean#parseBoolean(String)} (case-insensitive {@code "true"}, anything
     * else is {@code false}), or {@code defaultValue} if {@code key} is not set.
     *
     * @param properties the properties to read
     * @param key the property key
     * @param defaultValue the value to return if {@code key} is not set
     * @return the parsed value, or {@code defaultValue}
     * @throws NullPointerException if {@code properties} or {@code key} is
     *     {@code null}
     */
    public static boolean getBoolean(Properties properties, String key, boolean defaultValue) {
        return getString(properties, key).map(Boolean::parseBoolean).orElse(defaultValue);
    }

    private static Properties parse(String text) {
        Properties properties = new Properties();
        try (StringReader reader = new StringReader(text)) {
            properties.load(reader);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
        return properties;
    }
}
