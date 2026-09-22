package lithej.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.NotDirectoryException;
import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lithej.core.Validate;

/**
 * Directory-level helpers built on {@link java.nio.file.Files}: creation, listing,
 * size, and the destructive operations (emptying, recursive deletion, tree copy) that
 * deserve unambiguous names and explicit documentation.
 *
 * <p><b>Destructive operations:</b> {@link #empty(Path)} and
 * {@link #deleteRecursive(Path)} permanently delete file content with no recovery
 * mechanism. Their names say exactly what they do; there is no shorter alias for
 * either, on purpose.
 *
 * <p><b>Thread safety:</b> this class is stateless and thread-safe. It does not
 * synchronize concurrent access to the same directory; the guarantees are whatever the
 * underlying filesystem provides. Operations that walk a directory tree
 * ({@link #size}, {@link #listRecursive}, {@link #empty}, {@link #deleteRecursive},
 * {@link #copyTree}) are not atomic and can observe an inconsistent tree if it is
 * modified concurrently.
 */
public final class Directories {

    private static final String PARAM_DIR = "dir";

    private Directories() {
    }

    /**
     * Creates {@code dir} as a directory, including any missing parent directories.
     *
     * @param dir the directory to create
     * @throws NullPointerException if {@code dir} is {@code null}
     * @throws UncheckedIOException if {@code dir} already exists as a regular file
     *     (cause is a {@link FileAlreadyExistsException}), or creation fails for any
     *     other reason
     */
    public static void create(Path dir) {
        Validate.notNull(dir, PARAM_DIR);
        try {
            Files.createDirectories(dir);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Ensures {@code dir} exists as a directory: creates it (and any missing parents)
     * if absent, or does nothing if it already exists as a directory.
     *
     * @param dir the directory that must exist afterward
     * @throws NullPointerException if {@code dir} is {@code null}
     * @throws UncheckedIOException if {@code dir} already exists as a regular file, or
     *     creation fails for any other reason
     */
    public static void ensureExists(Path dir) {
        Validate.notNull(dir, PARAM_DIR);
        if (Files.isDirectory(dir)) {
            return;
        }
        create(dir);
    }

    /**
     * Lists the immediate children of {@code dir} (files and subdirectories, not
     * recursive).
     *
     * @param dir the directory to list
     * @return the immediate children of {@code dir}, in an unspecified order
     * @throws NullPointerException if {@code dir} is {@code null}
     * @throws UncheckedIOException if {@code dir} does not exist (cause is a
     *     {@link NoSuchFileException}), is not a directory (cause is a
     *     {@link NotDirectoryException}), or listing fails for any other reason
     */
    public static List<Path> list(Path dir) {
        Validate.notNull(dir, PARAM_DIR);
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Recursively lists every descendant of {@code dir} (files and subdirectories at
     * every depth), not including {@code dir} itself.
     *
     * @param dir the directory to walk
     * @return every descendant of {@code dir}, in an unspecified order
     * @throws NullPointerException if {@code dir} is {@code null}
     * @throws UncheckedIOException if the walk fails
     */
    public static List<Path> listRecursive(Path dir) {
        Validate.notNull(dir, PARAM_DIR);
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.filter(p -> !p.equals(dir)).collect(Collectors.toList());
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Computes the total size, in bytes, of every regular file under {@code dir}.
     *
     * @param dir the directory to measure
     * @return the sum of the sizes of every regular file found under {@code dir}
     * @throws NullPointerException if {@code dir} is {@code null}
     * @throws UncheckedIOException if the walk fails
     */
    public static long size(Path dir) {
        Validate.notNull(dir, PARAM_DIR);
        try (Stream<Path> stream = Files.walk(dir)) {
            return stream.filter(Files::isRegularFile).mapToLong(Directories::sizeOrZero).sum();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Returns {@code true} if {@code dir} has no entries.
     *
     * @param dir the directory to check
     * @return {@code true} if {@code dir} contains no files or subdirectories
     * @throws NullPointerException if {@code dir} is {@code null}
     * @throws UncheckedIOException if {@code dir} does not exist or is not a
     *     directory, or the check fails for any other reason
     */
    public static boolean isEmpty(Path dir) {
        Validate.notNull(dir, PARAM_DIR);
        try (Stream<Path> stream = Files.list(dir)) {
            return stream.findAny().isEmpty();
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Deletes every file and subdirectory inside {@code dir}, but keeps {@code dir}
     * itself. This permanently destroys data with no recovery mechanism.
     *
     * @param dir the directory to empty
     * @throws NullPointerException if {@code dir} is {@code null}
     * @throws UncheckedIOException if {@code dir} does not exist, is not a directory,
     *     or deletion fails for any reason (e.g. a file is locked by another process)
     */
    public static void empty(Path dir) {
        Validate.notNull(dir, PARAM_DIR);
        try (Stream<Path> stream = Files.list(dir)) {
            for (Path child : stream.collect(Collectors.toList())) {
                deleteTree(child);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    /**
     * Deletes {@code dir} and everything inside it. This permanently destroys data
     * with no recovery mechanism.
     *
     * @param dir the directory to delete
     * @throws NullPointerException if {@code dir} is {@code null}
     * @throws UncheckedIOException if {@code dir} does not exist, or deletion fails
     *     for any reason
     */
    public static void deleteRecursive(Path dir) {
        Validate.notNull(dir, PARAM_DIR);
        deleteTree(dir);
    }

    /**
     * Recursively copies every file and subdirectory from {@code source} into
     * {@code target}, creating {@code target} if it does not exist.
     *
     * @param source the directory tree to copy
     * @param target the destination directory; if it already exists, it must be empty
     * @throws NullPointerException if either argument is {@code null}
     * @throws UncheckedIOException if {@code source} does not exist or is not a
     *     directory; if {@code target} already exists and is not an empty directory
     *     (cause is a {@link FileAlreadyExistsException}); or if copying fails for any
     *     other reason
     */
    public static void copyTree(Path source, Path target) {
        Validate.notNull(source, "source");
        Validate.notNull(target, "target");
        if (Files.exists(target) && (!Files.isDirectory(target) || !isEmpty(target))) {
            throw new UncheckedIOException(new FileAlreadyExistsException(
                    target.toString(), null, "target already exists and is not an empty directory"));
        }
        try (Stream<Path> stream = Files.walk(source)) {
            for (Path path : stream.collect(Collectors.toList())) {
                Path destination = target.resolve(source.relativize(path));
                if (Files.isDirectory(path)) {
                    Files.createDirectories(destination);
                } else {
                    Path destinationParent = destination.getParent();
                    if (destinationParent != null) {
                        Files.createDirectories(destinationParent);
                    }
                    Files.copy(path, destination);
                }
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static void deleteTree(Path root) {
        try (Stream<Path> stream = Files.walk(root)) {
            List<Path> all = stream.sorted(Comparator.reverseOrder()).collect(Collectors.toList());
            for (Path path : all) {
                Files.delete(path);
            }
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static long sizeOrZero(Path file) {
        try {
            return Files.size(file);
        } catch (IOException e) {
            return 0L;
        }
    }
}
