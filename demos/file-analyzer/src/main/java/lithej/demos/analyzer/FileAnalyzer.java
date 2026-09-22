package lithej.demos.analyzer;

import java.nio.file.Path;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import lithej.collections.Lists;
import lithej.console.Console;
import lithej.io.FileIO;
import lithej.text.Text;

/**
 * Walks a directory tree and reports file counts and total size by extension, plus
 * the most common words across recognized text files.
 *
 * <p>Demonstrates: {@code lithej.io} (recursive walking, extension/size lookup),
 * {@code lithej.text} (word splitting), and {@code lithej.collections} (grouping and
 * counting), built entirely on LitheJ's public API.
 *
 * <p>Usage: {@code java -jar file-analyzer.jar [directory]} (defaults to the current
 * directory).
 */
public final class FileAnalyzer {

    private static final Set<String> TEXT_EXTENSIONS =
            Set.of("txt", "md", "java", "xml", "json", "csv", "properties", "yml", "yaml");
    private static final int TOP_WORD_COUNT = 10;
    private static final int MIN_WORD_LENGTH = 3;

    private FileAnalyzer() {
    }

    public static void main(String[] args) {
        Path root = args.length > 0 ? Path.of(args[0]) : Path.of(".");
        Console.println("Analyzing " + root.toAbsolutePath() + " ...");
        Console.println();

        List<Path> files = FileIO.walk(root);
        if (files.isEmpty()) {
            Console.println("No files found.");
            return;
        }

        reportByExtension(files);
        reportTopWords(files);
    }

    private static void reportByExtension(List<Path> files) {
        Map<String, List<Path>> byExtension =
                Lists.groupBy(files, path -> FileIO.extension(path).orElse("(no extension)"));

        Console.println("=== Files by extension ===");
        List<String> extensions = Lists.sortBy(byExtension.keySet(), Comparator.naturalOrder());
        for (String extension : extensions) {
            List<Path> group = byExtension.get(extension);
            long totalSize = group.stream().mapToLong(FileIO::size).sum();
            Console.printf("  .%-12s %4d files, %8d bytes%n", extension, group.size(), totalSize);
        }
        Console.println();
    }

    private static void reportTopWords(List<Path> files) {
        List<Path> textFiles = Lists.filter(files,
                path -> TEXT_EXTENSIONS.contains(FileIO.extension(path).orElse("")));

        List<String> allWords = textFiles.stream()
                .flatMap(path -> Text.words(FileIO.read(path)).stream())
                .map(word -> word.toLowerCase(Locale.ROOT))
                .filter(word -> word.length() >= MIN_WORD_LENGTH)
                .toList();

        if (allWords.isEmpty()) {
            Console.println("No recognized text files to analyze for word frequency.");
            return;
        }

        Map<String, Long> wordCounts = Lists.countBy(allWords, word -> word);
        List<Map.Entry<String, Long>> ranked = wordCounts.entrySet().stream()
                .sorted(Map.Entry.<String, Long>comparingByValue().reversed())
                .limit(TOP_WORD_COUNT)
                .toList();

        Console.println("=== Top " + TOP_WORD_COUNT + " words (" + textFiles.size()
                + " text files, " + MIN_WORD_LENGTH + "+ letters) ===");
        for (Map.Entry<String, Long> entry : ranked) {
            Console.printf("  %-20s %d%n", entry.getKey(), entry.getValue());
        }
    }
}
