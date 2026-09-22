package lithej.demos.students;

import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import lithej.collections.Lists;
import lithej.console.Console;
import lithej.io.FileIO;
import lithej.text.Text;

/**
 * A small interactive CLI that collects student records, reports pass/fail
 * statistics, and saves everything to a CSV file.
 *
 * <p>Demonstrates: {@code lithej.console} (validated, retrying prompts),
 * {@code lithej.collections} (sorting, partitioning), and {@code lithej.io}
 * (writing the report), built entirely on LitheJ's public API.
 */
public final class StudentRecordCli {

    private StudentRecordCli() {
    }

    public static void main(String[] args) {
        Console.println("=== Student Record CLI ===");
        Console.println("Enter student records. Leave the name blank to finish.");
        Console.println();

        List<StudentRecord> students = collectStudents();

        if (students.isEmpty()) {
            Console.println("No students entered. Nothing to report.");
            return;
        }

        report(students);
        save(students);
    }

    private static List<StudentRecord> collectStudents() {
        List<StudentRecord> students = new ArrayList<>();
        while (true) {
            String name = Console.ask("Student name (blank to finish): ");
            if (Text.isBlank(name)) {
                break;
            }
            int grade = Console.askInt("  Grade for " + name + " (0-100): ");
            if (grade < 0 || grade > 100) {
                Console.println("  Grade must be between 0 and 100. Skipping " + name + ".");
                continue;
            }
            students.add(new StudentRecord(name, grade));
        }
        return students;
    }

    private static void report(List<StudentRecord> students) {
        Console.println();
        Console.println("=== Report ===");

        List<StudentRecord> byGrade = Lists.sortBy(students, Comparator.comparingInt(StudentRecord::grade).reversed());
        Console.println("Ranked by grade:");
        for (StudentRecord student : byGrade) {
            Console.println("  " + student);
        }

        Map<Boolean, List<StudentRecord>> passFail = Lists.partition(students, StudentRecord::passing);
        int passing = passFail.getOrDefault(true, List.of()).size();
        int failing = passFail.getOrDefault(false, List.of()).size();

        double average = students.stream().mapToInt(StudentRecord::grade).average().orElse(0);
        Console.printf("%nAverage grade: %.1f%n", average);
        Console.println("Passing: " + passing + " / " + students.size());
        Console.println("Failing: " + failing + " / " + students.size());
    }

    private static void save(List<StudentRecord> students) {
        List<String> lines = new ArrayList<>();
        lines.add("name,grade,status");
        for (StudentRecord student : students) {
            lines.add(student.name() + "," + student.grade() + "," + (student.passing() ? "pass" : "fail"));
        }

        Path output = Path.of("student-report.csv");
        FileIO.writeLines(output, lines);
        Console.println();
        Console.println("Saved report to " + output.toAbsolutePath());
    }
}
