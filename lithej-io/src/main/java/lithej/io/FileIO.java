package lithej.io;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.nio.file.StandardOpenOption;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lithej.core.Validate;

/**
 * File read/write helpers built on {@link java.nio.file.Files}, defaulting to UTF-8 and
 * making overwrite/create behavior explicit in each method's name and Javadoc rather
 * than leaving it implicit.
 *
 * <p><b>Overwrite policy:</b> {@link #write(Path, String)} and {@link #writeLines} and
 * {@link #writeBytes} overwrite an existing target file, matching the behavior of
 * {@link Files#writeString(Path, CharSequence, java.nio.file.OpenOption...)}; this is
 * documented on each method rather than left as a surprise. {@link #copy(Path, Path)}
 * and {@link #move(Path, Path)}, by contrast, refuse to overwrite an existing target
 * (they throw {@link FileAlreadyExistsException}) unless you explicitly opt in via the
 * {@code overwrite} boolean overload — copying/moving over an existing file is far more
 * likely to be an accident than writing new content to a path you named yourself.
 *
 * <p><b>Parent directories:</b> every write method in this class creates missing parent
 * directories automatically (via {@link Files#createDirectories}). This never deletes
 * or overwrites anything; it only adds directories that did not exist.
 *
 * <p><b>Thread safety:</b> this class is stateless and thread-safe. It does not
 * synchronize concurrent access to the same file; the guarantees are whatever the
 * underlying filesystem provides.
 */
public final class FileIO {

    private static final String PARAM_PATH = "path";
    private static final String PARAM_CONTENT = "content";

    private FileIO() {
    }

    /**
     * Reads the entire file at {@code path} as a UTF-8 string.
     *
     * @param path the file to read
     * @return the file's content
     * @throws NullPointerException if {@code path} is {@code null}
     * @throws UncheckedIOException if {@code path} does not exist (cause is a
     *     {@link NoSuchFileException}), or reading fails for any other reason
     */
    public static String read(Path path) {
        return read(path, StandardCharsets.UTF_8);
    }

    /**
     * Reads the entire file at {@code path} as a string, decoded using {@code charset}.
     *
     * @param path the file to read
     * @param charset the charset to decode with
     * @return the file's content
     * @throws NullPointerException if either argument is {@code null}
     * @throws UncheckedIOException if {@code path} does not exist (cause is a
     *     {@link NoSuchFileException}), or reading fails for any other reason
     */
    public static String read(Path path, Charset charset) {
        Validate.notNull(path, PARAM_PATH);
        Validate.notNull(charset, "charset");
        try {
            return Files.readString(path, charset);
        } catch (IOException e) {
            throw wrap(e, path);
        }
    }

    /**
     * Reads the file at {@code path} as a list of lines, decoded as UTF-8. Recognizes
     * {@code \n}, {@code \r}, and {@code \r\n} as line terminators.
     *
     * @param path the file to read
     * @return the file's lines, without line terminators
     * @throws NullPointerException if {@code path} is {@code null}
     * @throws UncheckedIOException if {@code path} does not exist (cause is a
     *     {@link NoSuchFileException}), or reading fails for any other reason
     */
    public static List<String> readLines(Path path) {
        return readLines(path, StandardCharsets.UTF_8);
    }

    /**
     * Reads the file at {@code path} as a list of lines, decoded using {@code charset}.
     *
     * @param path the file to read
     * @param charset the charset to decode with
     * @return the file's lines, without line terminators
     * @throws NullPointerException if either argument is {@code null}
     * @throws UncheckedIOException if {@code path} does not exist (cause is a
     *     {@link NoSuchFileException}), or reading fails for any other reason
     */
    public static List<String> readLines(Path path, Charset charset) {
        Validate.notNull(path, PARAM_PATH);
        Validate.notNull(charset, "charset");
        try {
            return Files.readAllLines(path, charset);
        } catch (IOException e) {
            throw wrap(e, path);
        }
    }

    /**
     * Reads the entire file at {@code path} as raw bytes.
     *
     * @param path the file to read
     * @return the file's content
     * @throws NullPointerException if {@code path} is {@code null}
     * @throws UncheckedIOException if {@code path} does not exist (cause is a
     *     {@link NoSuchFileException}), or reading fails for any other reason
     */
    public static byte[] readBytes(Path path) {
        Validate.notNull(path, PARAM_PATH);
        try {
            return Files.readAllBytes(path);
        } catch (IOException e) {
            throw wrap(e, path);
        }
    }

    /**
     * Writes {@code content} to {@code path} as UTF-8, creating missing parent
     * directories and overwriting any existing file at {@code path}.
     *
     * @param path the file to write
     * @param content the text to write
     * @throws NullPointerException if either argument is {@code null}
     * @throws UncheckedIOException if writing fails
     */
    public static void write(Path path, String content) {
        Validate.notNull(path, PARAM_PATH);
        Validate.notNull(content, PARAM_CONTENT);
        createParentDirectories(path);
        try {
            Files.writeString(path, content, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw wrap(e, path);
        }
    }

    /**
     * Writes {@code lines} to {@code path} as UTF-8, one per line separated by
     * {@code \n}, creating missing parent directories and overwriting any existing
     * file at {@code path}.
     *
     * @param path the file to write
     * @param lines the lines to write
     * @throws NullPointerException if either argument is {@code null}
     * @throws UncheckedIOException if writing fails
     */
    public static void writeLines(Path path, List<String> lines) {
        Validate.notNull(path, PARAM_PATH);
        Validate.notNull(lines, "lines");
        createParentDirectories(path);
        try {
            Files.write(path, lines, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw wrap(e, path);
        }
    }

    /**
     * Writes {@code content} to {@code path} as raw bytes, creating missing parent
     * directories and overwriting any existing file at {@code path}.
     *
     * @param path the file to write
     * @param content the bytes to write
     * @throws NullPointerException if either argument is {@code null}
     * @throws UncheckedIOException if writing fails
     */
    public static void writeBytes(Path path, byte[] content) {
        Validate.notNull(path, PARAM_PATH);
        Validate.notNull(content, PARAM_CONTENT);
        createParentDirectories(path);
        try {
            Files.write(path, content);
        } catch (IOException e) {
            throw wrap(e, path);
        }
    }

    /**
     * Appends {@code content} to the end of {@code path} as UTF-8, creating the file
     * (and missing parent directories) if it does not already exist.
     *
     * @param path the file to append to
     * @param content the text to append
     * @throws NullPointerException if either argument is {@code null}
     * @throws UncheckedIOException if writing fails
     */
    public static void append(Path path, String content) {
        Validate.notNull(path, PARAM_PATH);
        Validate.notNull(content, PARAM_CONTENT);
        createParentDirectories(path);
        try {
            Files.writeString(
                    path, content, StandardCharsets.UTF_8, StandardOpenOption.CREATE, StandardOpenOption.APPEND);
        } catch (IOException e) {
            throw wrap(e, path);
        }
    }

    /**
     * Writes {@code content} to {@code path} atomically: the new content is written to
     * a temporary file in the same directory, then moved into place with
     * {@link java.nio.file.StandardCopyOption#ATOMIC_MOVE}, so concurrent readers never
     * observe a partially-written file. If the filesystem does not support atomic moves
     * across the two paths, falls back to a plain overwrite (not atomic, but still
     * correct) rather than failing outright.
     *
     * @param path the file to write
     * @param content the text to write, as UTF-8
     * @throws NullPointerException if either argument is {@code null}
     * @throws UncheckedIOException if writing fails
     */
    public static void writeAtomic(Path path, String content) {
        Validate.notNull(path, PARAM_PATH);
        Validate.notNull(content, PARAM_CONTENT);
        createParentDirectories(path);
        Path parent = path.toAbsolutePath().getParent();
        if (parent == null) {
            throw new IllegalArgumentException("path has no parent directory: " + path);
        }
        try {
            Path temp = Files.createTempFile(parent, ".lithej-", ".tmp");
            try {
                Files.writeString(temp, content, StandardCharsets.UTF_8);
                try {
                    Files.move(
                            temp,
                            path,
                            StandardCopyOption.ATOMIC_MOVE,
                            StandardCopyOption.REPLACE_EXISTING);
                } catch (java.nio.file.AtomicMoveNotSupportedException notSupported) {
                    Files.move(temp, path, StandardCopyOption.REPLACE_EXISTING);
                }
            } finally {
                Files.deleteIfExists(temp);
            }
        } catch (IOException e) {
            throw wrap(e, path);
        }
    }

    /**
     * Copies {@code source} to {@code target}, refusing to overwrite an existing file
     * at {@code target}.
     *
     * @param source the file to copy
     * @param target the destination path
     * @throws NullPointerException if either argument is {@code null}
     * @throws UncheckedIOException if {@code source} does not exist (cause is a
     *     {@link NoSuchFileException}); if {@code target} already exists (cause is a
     *     {@link FileAlreadyExistsException} — use {@link #copy(Path, Path, boolean)}
     *     to allow overwriting); or if copying fails for any other reason
     */
    public static void copy(Path source, Path target) {
        copy(source, target, false);
    }

    /**
     * Copies {@code source} to {@code target}.
     *
     * @param source the file to copy
     * @param target the destination path
     * @param overwrite if {@code true}, an existing file at {@code target} is replaced;
     *     if {@code false}, an existing file at {@code target} causes
     *     {@link FileAlreadyExistsException}
     * @throws NullPointerException if {@code source} or {@code target} is {@code null}
     * @throws UncheckedIOException if {@code source} does not exist (cause is a
     *     {@link NoSuchFileException}); if {@code target} exists and {@code overwrite}
     *     is {@code false} (cause is a {@link FileAlreadyExistsException}); or if
     *     copying fails for any other reason
     */
    public static void copy(Path source, Path target, boolean overwrite) {
        Validate.notNull(source, "source");
        Validate.notNull(target, "target");
        createParentDirectories(target);
        try {
            if (overwrite) {
                Files.copy(source, target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.copy(source, target);
            }
        } catch (IOException e) {
            throw wrap(e, target);
        }
    }

    /**
     * Moves (renames) {@code source} to {@code target}, refusing to overwrite an
     * existing file at {@code target}.
     *
     * @param source the file to move
     * @param target the destination path
     * @throws NullPointerException if either argument is {@code null}
     * @throws UncheckedIOException if {@code source} does not exist (cause is a
     *     {@link NoSuchFileException}); if {@code target} already exists (cause is a
     *     {@link FileAlreadyExistsException} — use {@link #move(Path, Path, boolean)}
     *     to allow overwriting); or if moving fails for any other reason
     */
    public static void move(Path source, Path target) {
        move(source, target, false);
    }

    /**
     * Moves (renames) {@code source} to {@code target}.
     *
     * @param source the file to move
     * @param target the destination path
     * @param overwrite if {@code true}, an existing file at {@code target} is replaced;
     *     if {@code false}, an existing file at {@code target} causes
     *     {@link FileAlreadyExistsException}
     * @throws NullPointerException if {@code source} or {@code target} is {@code null}
     * @throws UncheckedIOException if {@code source} does not exist (cause is a
     *     {@link NoSuchFileException}); if {@code target} exists and {@code overwrite}
     *     is {@code false} (cause is a {@link FileAlreadyExistsException}); or if
     *     moving fails for any other reason
     */
    public static void move(Path source, Path target, boolean overwrite) {
        Validate.notNull(source, "source");
        Validate.notNull(target, "target");
        createParentDirectories(target);
        try {
            if (overwrite) {
                Files.move(source, target, StandardCopyOption.REPLACE_EXISTING);
            } else {
                Files.move(source, target);
            }
        } catch (IOException e) {
            throw wrap(e, target);
        }
    }

    /**
     * Deletes the file (or empty directory) at {@code path}.
     *
     * @param path the path to delete
     * @throws NullPointerException if {@code path} is {@code null}
     * @throws UncheckedIOException if {@code path} does not exist (cause is a
     *     {@link NoSuchFileException} — use {@link #deleteIfExists(Path)} if that
     *     should be silently ignored), or if deletion fails for any other reason
     *     (e.g. a non-empty directory)
     */
    public static void delete(Path path) {
        Validate.notNull(path, PARAM_PATH);
        try {
            Files.delete(path);
        } catch (IOException e) {
            throw wrap(e, path);
        }
    }

    /**
     * Deletes the file (or empty directory) at {@code path} if it exists.
     *
     * @param path the path to delete
     * @return {@code true} if a file was deleted, {@code false} if {@code path} did
     *     not exist
     * @throws NullPointerException if {@code path} is {@code null}
     * @throws UncheckedIOException if deletion fails for a reason other than absence
     *     (e.g. a non-empty directory)
     */
    public static boolean deleteIfExists(Path path) {
        Validate.notNull(path, PARAM_PATH);
        try {
            return Files.deleteIfExists(path);
        } catch (IOException e) {
            throw wrap(e, path);
        }
    }

    /**
     * Returns {@code true} if {@code path} exists.
     *
     * @param path the path to check
     * @return {@code true} if {@code path} exists
     * @throws NullPointerException if {@code path} is {@code null}
     */
    public static boolean exists(Path path) {
        Validate.notNull(path, PARAM_PATH);
        return Files.exists(path);
    }

    /**
     * Returns the size, in bytes, of the file at {@code path}.
     *
     * @param path the file to measure
     * @return the file size in bytes
     * @throws NullPointerException if {@code path} is {@code null}
     * @throws UncheckedIOException if {@code path} does not exist (cause is a
     *     {@link NoSuchFileException}), or the size cannot be determined for any
     *     other reason
     */
    public static long size(Path path) {
        Validate.notNull(path, PARAM_PATH);
        try {
            return Files.size(path);
        } catch (IOException e) {
            throw wrap(e, path);
        }
    }

    /**
     * Returns the file extension of {@code path} (the substring after the last
     * {@code .} in the file name), without the leading dot.
     *
     * @param path the path to inspect
     * @return the extension (e.g. {@code "txt"}), or {@link Optional#empty()} if the
     *     file name has no {@code .} or ends with one
     * @throws NullPointerException if {@code path} is {@code null}
     */
    public static Optional<String> extension(Path path) {
        String name = name(path);
        int dot = name.lastIndexOf('.');
        if (dot < 0 || dot == name.length() - 1) {
            return Optional.empty();
        }
        return Optional.of(name.substring(dot + 1));
    }

    /**
     * Returns the file name component of {@code path} (the last path segment).
     *
     * @param path the path to inspect
     * @return the file name, or an empty string if {@code path} has no name elements
     *     (e.g. the root path)
     * @throws NullPointerException if {@code path} is {@code null}
     */
    public static String name(Path path) {
        Validate.notNull(path, PARAM_PATH);
        Path fileName = path.getFileName();
        return fileName == null ? "" : fileName.toString();
    }

    /**
     * Recursively lists every regular file under {@code root} (not directories),
     * eagerly collected into a list.
     *
     * <p>For very large trees where you want to process entries lazily instead of
     * materializing them all, use {@link java.nio.file.Files#walk(Path, java.nio.file.FileVisitOption...)}
     * directly, remembering to close the returned stream.
     *
     * @param root the directory to walk
     * @return every regular file found under {@code root}, in an unspecified order
     * @throws NullPointerException if {@code root} is {@code null}
     * @throws UncheckedIOException if the walk fails
     */
    public static List<Path> walk(Path root) {
        Validate.notNull(root, "root");
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile).collect(Collectors.toList());
        } catch (IOException e) {
            throw wrap(e, root);
        }
    }

    /**
     * Recursively finds every regular file under {@code root} whose path satisfies
     * {@code predicate}, eagerly collected into a list.
     *
     * @param root the directory to search
     * @param predicate the condition a file's path must satisfy to be included
     * @return the matching files, in an unspecified order
     * @throws NullPointerException if either argument is {@code null}
     * @throws UncheckedIOException if the search fails
     */
    public static List<Path> find(Path root, java.util.function.Predicate<Path> predicate) {
        Validate.notNull(root, "root");
        Validate.notNull(predicate, "predicate");
        try (Stream<Path> stream = Files.walk(root)) {
            return stream.filter(Files::isRegularFile).filter(predicate).collect(Collectors.toList());
        } catch (IOException e) {
            throw wrap(e, root);
        }
    }

    /**
     * Creates a new, empty temporary file that is deleted automatically when the JVM
     * exits normally.
     *
     * @param prefix the filename prefix, may be {@code null} to use no prefix
     * @param suffix the filename suffix (e.g. {@code ".txt"}), may be {@code null} to
     *     default to {@code ".tmp"}
     * @return the path of the newly created file
     * @throws UncheckedIOException if the file cannot be created
     */
    public static Path tempFile(String prefix, String suffix) {
        try {
            Path file = Files.createTempFile(prefix, suffix);
            file.toFile().deleteOnExit();
            return file;
        } catch (IOException e) {
            throw wrap(e, null);
        }
    }

    /**
     * Creates a new, empty temporary directory that is <b>not</b> deleted
     * automatically (recursive deletion of a possibly-populated directory on JVM exit
     * is unsafe to do implicitly). Delete it yourself with
     * {@code Directories.deleteRecursive(...)} when done.
     *
     * @param prefix the directory name prefix, may be {@code null} to use no prefix
     * @return the path of the newly created directory
     * @throws UncheckedIOException if the directory cannot be created
     */
    public static Path tempDirectory(String prefix) {
        try {
            return Files.createTempDirectory(prefix);
        } catch (IOException e) {
            throw wrap(e, null);
        }
    }

    private static void createParentDirectories(Path path) {
        Path parent = path.toAbsolutePath().getParent();
        if (parent != null) {
            try {
                Files.createDirectories(parent);
            } catch (IOException e) {
                throw wrap(e, parent);
            }
        }
    }

    private static UncheckedIOException wrap(IOException e, Path path) {
        if (e instanceof NoSuchFileException || e instanceof FileAlreadyExistsException) {
            return new UncheckedIOException(e);
        }
        String suffix = path == null ? "" : " (" + path + ")";
        return new UncheckedIOException(e.getMessage() + suffix, e);
    }
}
