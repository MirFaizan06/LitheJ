package lithej.io;

import java.io.Serial;

/**
 * Thrown by {@link Resources} when a requested classpath resource cannot be found.
 *
 * <p>The JDK's own classpath resource APIs (e.g.
 * {@link ClassLoader#getResourceAsStream(String)}) return {@code null} on a missing
 * resource, which is easy to forget to check and produces a confusing
 * {@link NullPointerException} far from the real cause. This exception makes that
 * failure explicit and immediate.
 */
public final class ResourceNotFoundException extends RuntimeException {

    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Creates an exception naming the missing resource.
     *
     * @param resourcePath the classpath-relative path that could not be found
     */
    public ResourceNotFoundException(String resourcePath) {
        super("Resource not found on classpath: " + resourcePath);
    }
}
