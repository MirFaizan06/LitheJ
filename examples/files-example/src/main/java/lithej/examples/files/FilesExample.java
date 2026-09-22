package lithej.examples.files;

import java.nio.file.Path;
import java.util.List;
import lithej.io.Directories;
import lithej.io.FileIO;

/**
 * Demonstrates {@link FileIO} and {@link Directories}: writing/reading UTF-8 text,
 * appending, recursively walking a directory, and cleaning up afterward.
 */
public final class FilesExample {

    private FilesExample() {
    }

    public static void main(String[] args) {
        Path workDir = FileIO.tempDirectory("lithej-files-example-");
        try {
            Path notes = workDir.resolve("notes.txt");
            FileIO.writeLines(notes, List.of("first line", "second line"));
            FileIO.append(notes, "third line\n");

            System.out.println("notes.txt contains:");
            FileIO.readLines(notes).forEach(line -> System.out.println("  " + line));

            Path reportsDir = workDir.resolve("reports");
            FileIO.write(reportsDir.resolve("january.csv"), "month,total\njan,120\n");
            FileIO.write(reportsDir.resolve("february.csv"), "month,total\nfeb,95\n");

            System.out.println("Files under " + workDir + ":");
            for (Path file : FileIO.walk(workDir)) {
                System.out.println("  " + workDir.relativize(file) + " (" + FileIO.size(file) + " bytes)");
            }

            System.out.println("Total size: " + Directories.size(workDir) + " bytes");
        } finally {
            Directories.deleteRecursive(workDir);
        }
    }
}
