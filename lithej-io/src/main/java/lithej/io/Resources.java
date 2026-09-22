package lithej.io;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lithej.core.Validate;

/**
 * Read access to resources bundled on the classpath (inside a JAR or an exploded
 * classes directory), defaulting to UTF-8 for text.
 *
 * <p>Unlike {@link lithej.io.FileIO}, these methods do not accept a {@link java.nio.file.Path}:
 * a classpath resource packaged inside a JAR has no meaningful filesystem path, so
 * every method here takes a classpath-relative resource name instead (e.g.
 * {@code "config/defaults.properties"}, no leading {@code /}).
 *
 * <p>Resources are located using the current thread's context class loader when one is
 * set, falling back to the class loader that loaded {@link Resources} itself. This
 * matches the lookup strategy most application frameworks expect and works correctly
 * both when this library is on the application class path and when it is loaded by a
 * plugin/module class loader.
 *
 * <p><b>Thread safety:</b> this class is stateless and thread-safe.
 */
public final class Resources {

    private Resources() {
    }

    /**
     * Reads {@code resourcePath} as a UTF-8 string.
     *
     * @param resourcePath the classpath-relative resource path, no leading {@code /}
     * @return the resource's content
     * @throws NullPointerException if {@code resourcePath} is {@code null}
     * @throws ResourceNotFoundException if no resource exists at {@code resourcePath}
     * @throws UncheckedIOException if reading fails for any other reason
     */
    public static String read(String resourcePath) {
        return new String(readBytes(resourcePath), StandardCharsets.UTF_8);
    }

    /**
     * Reads {@code resourcePath} as a list of UTF-8 lines.
     *
     * @param resourcePath the classpath-relative resource path, no leading {@code /}
     * @return the resource's lines, without line terminators
     * @throws NullPointerException if {@code resourcePath} is {@code null}
     * @throws ResourceNotFoundException if no resource exists at {@code resourcePath}
     * @throws UncheckedIOException if reading fails for any other reason
     */
    public static List<String> readLines(String resourcePath) {
        return read(resourcePath).lines().toList();
    }

    /**
     * Reads {@code resourcePath} as raw bytes.
     *
     * @param resourcePath the classpath-relative resource path, no leading {@code /}
     * @return the resource's content
     * @throws NullPointerException if {@code resourcePath} is {@code null}
     * @throws ResourceNotFoundException if no resource exists at {@code resourcePath}
     * @throws UncheckedIOException if reading fails for any other reason
     */
    public static byte[] readBytes(String resourcePath) {
        try (InputStream in = open(resourcePath)) {
            return in.readAllBytes();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns {@code true} if a resource exists at {@code resourcePath}.
     *
     * @param resourcePath the classpath-relative resource path, no leading {@code /}
     * @return {@code true} if the resource can be found
     * @throws NullPointerException if {@code resourcePath} is {@code null}
     */
    public static boolean exists(String resourcePath) {
        Validate.notNull(resourcePath, "resourcePath");
        return classLoader().getResource(resourcePath) != null;
    }

    /**
     * Opens {@code resourcePath} as a stream. The caller is responsible for closing
     * the returned stream.
     *
     * @param resourcePath the classpath-relative resource path, no leading {@code /}
     * @return an open input stream over the resource's content
     * @throws NullPointerException if {@code resourcePath} is {@code null}
     * @throws ResourceNotFoundException if no resource exists at {@code resourcePath}
     */
    public static InputStream open(String resourcePath) {
        Validate.notNull(resourcePath, "resourcePath");
        InputStream in = classLoader().getResourceAsStream(resourcePath);
        if (in == null) {
            throw new ResourceNotFoundException(resourcePath);
        }
        return in;
    }

    private static ClassLoader classLoader() {
        ClassLoader contextLoader = Thread.currentThread().getContextClassLoader();
        return contextLoader != null ? contextLoader : Resources.class.getClassLoader();
    }
}
