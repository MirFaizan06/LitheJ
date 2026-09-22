package lithej.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.io.UncheckedIOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class DirectoriesTest {

    @TempDir
    Path tempDir;

    @Test
    void createMakesDirectoryIncludingParents() {
        Path dir = tempDir.resolve("a/b/c");
        Directories.create(dir);
        assertThat(Files.isDirectory(dir)).isTrue();
    }

    @Test
    void createRejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> Directories.create(null));
    }

    @Test
    void createOnExistingRegularFileThrows() {
        Path file = tempDir.resolve("blocker");
        FileIO.write(file, "x");
        assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> Directories.create(file));
    }

    @Test
    void ensureExistsOnExistingRegularFileThrows() {
        Path file = tempDir.resolve("blocker2");
        FileIO.write(file, "x");
        assertThatExceptionOfType(UncheckedIOException.class).isThrownBy(() -> Directories.ensureExists(file));
    }

    @Test
    void listOfMissingDirectoryThrows() {
        assertThatExceptionOfType(UncheckedIOException.class)
                .isThrownBy(() -> Directories.list(tempDir.resolve("missing")));
    }

    @Test
    void isEmptyOfMissingDirectoryThrows() {
        assertThatExceptionOfType(UncheckedIOException.class)
                .isThrownBy(() -> Directories.isEmpty(tempDir.resolve("missing")));
    }

    @Test
    void listRecursiveOfMissingDirectoryThrows() {
        assertThatExceptionOfType(UncheckedIOException.class)
                .isThrownBy(() -> Directories.listRecursive(tempDir.resolve("missing")));
    }

    @Test
    void sizeOfMissingDirectoryThrows() {
        assertThatExceptionOfType(UncheckedIOException.class)
                .isThrownBy(() -> Directories.size(tempDir.resolve("missing")));
    }

    @Test
    void emptyOfMissingDirectoryThrows() {
        assertThatExceptionOfType(UncheckedIOException.class)
                .isThrownBy(() -> Directories.empty(tempDir.resolve("missing")));
    }

    @Test
    void copyTreeOfMissingSourceThrows() {
        assertThatExceptionOfType(UncheckedIOException.class)
                .isThrownBy(() -> Directories.copyTree(tempDir.resolve("missing"), tempDir.resolve("target")));
    }

    @Test
    void nullArgumentsAreRejectedAcrossAllMethods() {
        assertThatNullPointerException().isThrownBy(() -> Directories.ensureExists(null));
        assertThatNullPointerException().isThrownBy(() -> Directories.list(null));
        assertThatNullPointerException().isThrownBy(() -> Directories.listRecursive(null));
        assertThatNullPointerException().isThrownBy(() -> Directories.size(null));
        assertThatNullPointerException().isThrownBy(() -> Directories.isEmpty(null));
        assertThatNullPointerException().isThrownBy(() -> Directories.empty(null));
        assertThatNullPointerException().isThrownBy(() -> Directories.deleteRecursive(null));
        assertThatNullPointerException().isThrownBy(() -> Directories.copyTree(null, tempDir));
        assertThatNullPointerException().isThrownBy(() -> Directories.copyTree(tempDir, null));
    }

    @Test
    void ensureExistsCreatesWhenMissing() {
        Path dir = tempDir.resolve("fresh");
        Directories.ensureExists(dir);
        assertThat(Files.isDirectory(dir)).isTrue();
    }

    @Test
    void ensureExistsIsNoOpWhenAlreadyADirectory() {
        Path dir = tempDir.resolve("already");
        Directories.create(dir);
        Directories.ensureExists(dir);
        assertThat(Files.isDirectory(dir)).isTrue();
    }

    @Test
    void listReturnsOnlyImmediateChildren() throws Exception {
        Files.createFile(tempDir.resolve("top1.txt"));
        Files.createFile(tempDir.resolve("top2.txt"));
        Files.createDirectory(tempDir.resolve("subdir"));
        Files.createFile(tempDir.resolve("subdir/nested.txt"));

        List<Path> children = Directories.list(tempDir);

        assertThat(children).extracting(Path::getFileName).extracting(Path::toString)
                .containsExactlyInAnyOrder("top1.txt", "top2.txt", "subdir");
    }

    @Test
    void listRecursiveReturnsAllDescendantsNotTheRootItself() throws Exception {
        Files.createDirectory(tempDir.resolve("sub"));
        Files.createFile(tempDir.resolve("sub/file.txt"));

        List<Path> descendants = Directories.listRecursive(tempDir);

        assertThat(descendants).hasSize(2);
        assertThat(descendants).doesNotContain(tempDir);
    }

    @Test
    void sizeSumsAllRegularFileSizesRecursively() {
        FileIO.write(tempDir.resolve("a.txt"), "12345");
        FileIO.write(tempDir.resolve("sub/b.txt"), "1234567890");

        assertThat(Directories.size(tempDir)).isEqualTo(15L);
    }

    @Test
    void isEmptyReflectsDirectoryState() {
        assertThat(Directories.isEmpty(tempDir)).isTrue();
        FileIO.write(tempDir.resolve("x.txt"), "x");
        assertThat(Directories.isEmpty(tempDir)).isFalse();
    }

    @Test
    void emptyRemovesAllContentsButKeepsDirectory() {
        FileIO.write(tempDir.resolve("a.txt"), "1");
        FileIO.write(tempDir.resolve("sub/b.txt"), "2");

        Directories.empty(tempDir);

        assertThat(Files.isDirectory(tempDir)).isTrue();
        assertThat(Directories.isEmpty(tempDir)).isTrue();
    }

    @Test
    void deleteRecursiveRemovesDirectoryAndContents() {
        Path dir = tempDir.resolve("victim");
        FileIO.write(dir.resolve("a.txt"), "1");
        FileIO.write(dir.resolve("sub/b.txt"), "2");

        Directories.deleteRecursive(dir);

        assertThat(Files.exists(dir)).isFalse();
    }

    @Test
    void deleteRecursiveOnMissingDirectoryThrows() {
        assertThatExceptionOfType(UncheckedIOException.class)
                .isThrownBy(() -> Directories.deleteRecursive(tempDir.resolve("nope")));
    }

    @Test
    void copyTreeCopiesFilesAndSubdirectoriesPreservingStructure() {
        Path source = tempDir.resolve("source");
        FileIO.write(source.resolve("a.txt"), "A");
        FileIO.write(source.resolve("nested/b.txt"), "B");
        Path target = tempDir.resolve("target");

        Directories.copyTree(source, target);

        assertThat(FileIO.read(target.resolve("a.txt"))).isEqualTo("A");
        assertThat(FileIO.read(target.resolve("nested/b.txt"))).isEqualTo("B");
    }

    @Test
    void copyTreeRefusesNonEmptyExistingTarget() {
        Path source = tempDir.resolve("source2");
        FileIO.write(source.resolve("a.txt"), "A");
        Path target = tempDir.resolve("target2");
        FileIO.write(target.resolve("existing.txt"), "existing");

        assertThatExceptionOfType(UncheckedIOException.class)
                .isThrownBy(() -> Directories.copyTree(source, target));
    }

    @Test
    void copyTreeAllowsEmptyExistingTarget() {
        Path source = tempDir.resolve("source3");
        FileIO.write(source.resolve("a.txt"), "A");
        Path target = tempDir.resolve("target3");
        Directories.create(target);

        Directories.copyTree(source, target);

        assertThat(FileIO.read(target.resolve("a.txt"))).isEqualTo("A");
    }
}
