package lithej.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileIOTest {

    @TempDir
    Path tempDir;

    @Test
    void writeThenReadRoundTrips() {
        Path file = tempDir.resolve("hello.txt");
        FileIO.write(file, "hello world");
        assertThat(FileIO.read(file)).isEqualTo("hello world");
    }

    @Test
    void writeCreatesMissingParentDirectories() {
        Path file = tempDir.resolve("a/b/c/deep.txt");
        FileIO.write(file, "deep");
        assertThat(FileIO.read(file)).isEqualTo("deep");
    }

    @Test
    void writeOverwritesExistingFile() {
        Path file = tempDir.resolve("f.txt");
        FileIO.write(file, "first");
        FileIO.write(file, "second");
        assertThat(FileIO.read(file)).isEqualTo("second");
    }

    @Test
    void readWithExplicitCharset() throws Exception {
        Path file = tempDir.resolve("latin1.txt");
        Files.write(file, "café".getBytes(StandardCharsets.ISO_8859_1));
        assertThat(FileIO.read(file, StandardCharsets.ISO_8859_1)).isEqualTo("café");
    }

    @Test
    void readMissingFileThrowsUncheckedIOExceptionWithNoSuchFileExceptionCause() {
        Path missing = tempDir.resolve("missing.txt");
        assertThatExceptionOfType(UncheckedIOException.class)
                .isThrownBy(() -> FileIO.read(missing))
                .withCauseInstanceOf(NoSuchFileException.class);
    }

    @Test
    void readRejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> FileIO.read(null));
        assertThatNullPointerException().isThrownBy(() -> FileIO.read(tempDir.resolve("x"), null));
    }

    @Test
    void readLinesOfMissingFileThrows() {
        assertThatExceptionOfType(UncheckedIOException.class)
                .isThrownBy(() -> FileIO.readLines(tempDir.resolve("missing.txt")))
                .withCauseInstanceOf(NoSuchFileException.class);
    }

    @Test
    void readBytesOfMissingFileThrows() {
        assertThatExceptionOfType(UncheckedIOException.class)
                .isThrownBy(() -> FileIO.readBytes(tempDir.resolve("missing.txt")))
                .withCauseInstanceOf(NoSuchFileException.class);
    }

    @Test
    void writingToADirectoryPathFailsWithWrappedIOException() {
        Path dir = tempDir.resolve("adir");
        Directories.create(dir);
        assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> FileIO.write(dir, "x"));
        assertThatExceptionOfType(UncheckedIOException.class)
                .isThrownBy(() -> FileIO.writeLines(dir, List.of("x")));
        assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> FileIO.writeBytes(dir, new byte[]{1}));
        assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> FileIO.append(dir, "x"));
    }

    @Test
    void deletingNonEmptyDirectoryThrowsWrappedIOException() {
        Path dir = tempDir.resolve("nonempty");
        FileIO.write(dir.resolve("child.txt"), "x");
        assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> FileIO.delete(dir));
    }

    @Test
    void walkOfMissingDirectoryThrows() {
        assertThatExceptionOfType(UncheckedIOException.class)
                .isThrownBy(() -> FileIO.walk(tempDir.resolve("missing-dir")));
    }

    @Test
    void findOfMissingDirectoryThrows() {
        assertThatExceptionOfType(UncheckedIOException.class)
                .isThrownBy(() -> FileIO.find(tempDir.resolve("missing-dir"), p -> true));
    }

    @Test
    void nameOfRootPathIsEmptyString() {
        Path root = tempDir.getRoot();
        assertThat(FileIO.name(root)).isEmpty();
    }

    @Test
    void writeLinesThenReadLinesRoundTrips() {
        Path file = tempDir.resolve("lines.txt");
        FileIO.writeLines(file, List.of("a", "b", "c"));
        assertThat(FileIO.readLines(file)).containsExactly("a", "b", "c");
    }

    @Test
    void readLinesHandlesEmptyFile() {
        Path file = tempDir.resolve("empty.txt");
        FileIO.write(file, "");
        assertThat(FileIO.readLines(file)).isEmpty();
    }

    @Test
    void writeBytesThenReadBytesRoundTrips() {
        Path file = tempDir.resolve("bin.dat");
        byte[] data = {1, 2, 3, (byte) 255, 0};
        FileIO.writeBytes(file, data);
        assertThat(FileIO.readBytes(file)).isEqualTo(data);
    }

    @Test
    void appendAddsToEndAndCreatesFileIfAbsent() {
        Path file = tempDir.resolve("log.txt");
        FileIO.append(file, "line1\n");
        FileIO.append(file, "line2\n");
        assertThat(FileIO.read(file)).isEqualTo("line1\nline2\n");
    }

    @Test
    void writeAtomicWritesFullContentAndOverwrites() {
        Path file = tempDir.resolve("atomic.txt");
        FileIO.writeAtomic(file, "version 1");
        assertThat(FileIO.read(file)).isEqualTo("version 1");
        FileIO.writeAtomic(file, "version 2");
        assertThat(FileIO.read(file)).isEqualTo("version 2");
    }

    @Test
    void copyRefusesToOverwriteByDefault() {
        Path source = tempDir.resolve("src.txt");
        Path target = tempDir.resolve("dst.txt");
        FileIO.write(source, "source content");
        FileIO.write(target, "existing content");

        assertThatExceptionOfType(UncheckedIOException.class)
                .isThrownBy(() -> FileIO.copy(source, target))
                .withCauseInstanceOf(FileAlreadyExistsException.class);
        assertThat(FileIO.read(target)).isEqualTo("existing content");
    }

    @Test
    void copyWithOverwriteTrueReplacesTarget() {
        Path source = tempDir.resolve("src2.txt");
        Path target = tempDir.resolve("dst2.txt");
        FileIO.write(source, "new content");
        FileIO.write(target, "old content");

        FileIO.copy(source, target, true);

        assertThat(FileIO.read(target)).isEqualTo("new content");
        assertThat(FileIO.read(source)).isEqualTo("new content");
    }

    @Test
    void copyToNewPathCreatesParentDirectories() {
        Path source = tempDir.resolve("src3.txt");
        Path target = tempDir.resolve("nested/dir/dst3.txt");
        FileIO.write(source, "content");
        FileIO.copy(source, target);
        assertThat(FileIO.read(target)).isEqualTo("content");
    }

    @Test
    void moveRefusesToOverwriteByDefault() {
        Path source = tempDir.resolve("m1.txt");
        Path target = tempDir.resolve("m2.txt");
        FileIO.write(source, "moving");
        FileIO.write(target, "existing");

        assertThatExceptionOfType(UncheckedIOException.class)
                .isThrownBy(() -> FileIO.move(source, target))
                .withCauseInstanceOf(FileAlreadyExistsException.class);
    }

    @Test
    void moveWithOverwriteTrueReplacesTargetAndRemovesSource() {
        Path source = tempDir.resolve("m3.txt");
        Path target = tempDir.resolve("m4.txt");
        FileIO.write(source, "moving content");
        FileIO.write(target, "old content");

        FileIO.move(source, target, true);

        assertThat(FileIO.exists(source)).isFalse();
        assertThat(FileIO.read(target)).isEqualTo("moving content");
    }

    @Test
    void deleteRemovesFile() {
        Path file = tempDir.resolve("d.txt");
        FileIO.write(file, "x");
        FileIO.delete(file);
        assertThat(FileIO.exists(file)).isFalse();
    }

    @Test
    void deleteMissingFileThrows() {
        Path missing = tempDir.resolve("nope.txt");
        assertThatExceptionOfType(UncheckedIOException.class)
                .isThrownBy(() -> FileIO.delete(missing))
                .withCauseInstanceOf(NoSuchFileException.class);
    }

    @Test
    void deleteIfExistsReturnsFalseForMissingFile() {
        assertThat(FileIO.deleteIfExists(tempDir.resolve("absent.txt"))).isFalse();
    }

    @Test
    void deleteIfExistsReturnsTrueAndDeletesExistingFile() {
        Path file = tempDir.resolve("present.txt");
        FileIO.write(file, "x");
        assertThat(FileIO.deleteIfExists(file)).isTrue();
        assertThat(FileIO.exists(file)).isFalse();
    }

    @Test
    void existsReflectsFileState() {
        Path file = tempDir.resolve("e.txt");
        assertThat(FileIO.exists(file)).isFalse();
        FileIO.write(file, "x");
        assertThat(FileIO.exists(file)).isTrue();
    }

    @Test
    void sizeReturnsByteLength() {
        Path file = tempDir.resolve("s.txt");
        FileIO.write(file, "12345");
        assertThat(FileIO.size(file)).isEqualTo(5L);
    }

    @Test
    void sizeOfMissingFileThrows() {
        assertThatExceptionOfType(UncheckedIOException.class)
                .isThrownBy(() -> FileIO.size(tempDir.resolve("missing-size.txt")));
    }

    @Test
    void extensionReturnsSuffixAfterLastDot() {
        assertThat(FileIO.extension(Path.of("archive.tar.gz"))).isEqualTo(Optional.of("gz"));
        assertThat(FileIO.extension(Path.of("readme"))).isEqualTo(Optional.empty());
        assertThat(FileIO.extension(Path.of("trailing.dot."))).isEqualTo(Optional.empty());
    }

    @Test
    void nameReturnsLastPathSegment() {
        assertThat(FileIO.name(Path.of("a/b/c.txt"))).isEqualTo("c.txt");
    }

    @Test
    void walkFindsAllRegularFilesRecursively() {
        FileIO.write(tempDir.resolve("top.txt"), "1");
        FileIO.write(tempDir.resolve("nested/inner.txt"), "2");
        FileIO.write(tempDir.resolve("nested/deep/deepest.txt"), "3");

        List<Path> found = FileIO.walk(tempDir);

        assertThat(found).extracting(Path::getFileName).extracting(Path::toString)
                .containsExactlyInAnyOrder("top.txt", "inner.txt", "deepest.txt");
    }

    @Test
    void findFiltersWalkResultsByPredicate() {
        FileIO.write(tempDir.resolve("a.txt"), "1");
        FileIO.write(tempDir.resolve("b.log"), "2");
        FileIO.write(tempDir.resolve("nested/c.txt"), "3");

        List<Path> txtFiles = FileIO.find(tempDir, p -> p.toString().endsWith(".txt"));

        assertThat(txtFiles).hasSize(2);
    }

    @Test
    void tempFileIsCreatedAndEmpty() {
        Path file = FileIO.tempFile("lithej-", ".tmp");
        try {
            assertThat(Files.exists(file)).isTrue();
            assertThat(Files.isRegularFile(file)).isTrue();
            assertThat(FileIO.size(file)).isZero();
        } finally {
            FileIO.deleteIfExists(file);
        }
    }

    @Test
    void tempDirectoryIsCreatedAndEmpty() {
        Path dir = FileIO.tempDirectory("lithej-dir-");
        try {
            assertThat(Files.isDirectory(dir)).isTrue();
            assertThat(FileIO.walk(dir)).isEmpty();
        } finally {
            Directories.deleteRecursive(dir);
        }
    }
}
