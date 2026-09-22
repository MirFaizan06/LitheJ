package lithej.console;

import java.util.List;
import java.util.NoSuchElementException;
import java.util.function.Predicate;

/**
 * Static convenience facade over the console attached to {@link System#in} and
 * {@link System#out}, for scripts and small command-line programs.
 *
 * <pre>{@code
 * int age = Console.askInt("Enter age: ");
 * Console.println("You are " + age + " years old.");
 * }</pre>
 *
 * <p>Every method here simply delegates to {@link ConsoleIO#system()}. If you need to
 * unit-test code that interacts with the console, construct your own
 * {@link ConsoleIO} over injected streams instead of using this class, and depend on
 * {@link ConsoleIO} in your code rather than calling {@code Console} directly.
 *
 * <p><b>Thread safety:</b> not thread-safe; see {@link ConsoleIO#system()}.
 */
public final class Console {

    private Console() {
    }

    /**
     * Writes {@code text} with no trailing newline. See {@link ConsoleIO#print(Object)}.
     *
     * @param text the text to write
     */
    public static void print(Object text) {
        ConsoleIO.system().print(text);
    }

    /**
     * Writes {@code text} followed by a newline. See
     * {@link ConsoleIO#println(Object)}.
     *
     * @param text the text to write
     */
    public static void println(Object text) {
        ConsoleIO.system().println(text);
    }

    /**
     * Writes a blank line.
     */
    public static void println() {
        ConsoleIO.system().println();
    }

    /**
     * Writes a formatted string. See {@link ConsoleIO#printf(String, Object...)}.
     *
     * @param format the format string
     * @param args the format arguments
     */
    public static void printf(String format, Object... args) {
        ConsoleIO.system().printf(format, args);
    }

    /**
     * Prints {@code prompt} and reads a line of input. See
     * {@link ConsoleIO#ask(String)}.
     *
     * @param prompt the text to print before reading
     * @return the line read
     * @throws NoSuchElementException if input is exhausted
     */
    public static String ask(String prompt) {
        return ConsoleIO.system().ask(prompt);
    }

    /**
     * Prints {@code prompt} and reads input until {@code validator} accepts it. See
     * {@link ConsoleIO#ask(String, Predicate, String)}.
     *
     * @param prompt the text to print before reading
     * @param validator predicate the input must satisfy
     * @param errorMessage message shown after invalid input
     * @return the first valid line read
     * @throws NoSuchElementException if input is exhausted
     */
    public static String ask(String prompt, Predicate<String> validator, String errorMessage) {
        return ConsoleIO.system().ask(prompt, validator, errorMessage);
    }

    /**
     * Prints {@code prompt} and reads input until it parses as an {@code int}. See
     * {@link ConsoleIO#askInt(String)}.
     *
     * @param prompt the text to print before reading
     * @return the parsed integer
     * @throws NoSuchElementException if input is exhausted
     */
    public static int askInt(String prompt) {
        return ConsoleIO.system().askInt(prompt);
    }

    /**
     * Prints {@code prompt} and reads input until it parses as an {@code int}, up to
     * {@code maxAttempts} tries. See {@link ConsoleIO#askInt(String, int)}.
     *
     * @param prompt the text to print before reading
     * @param maxAttempts the maximum number of attempts, or negative for no limit
     * @return the parsed integer
     * @throws NoSuchElementException if input is exhausted
     * @throws IllegalStateException if {@code maxAttempts} is reached
     */
    public static int askInt(String prompt, int maxAttempts) {
        return ConsoleIO.system().askInt(prompt, maxAttempts);
    }

    /**
     * Prints {@code prompt} and reads input until it parses as a {@code long}. See
     * {@link ConsoleIO#askLong(String)}.
     *
     * @param prompt the text to print before reading
     * @return the parsed long
     * @throws NoSuchElementException if input is exhausted
     */
    public static long askLong(String prompt) {
        return ConsoleIO.system().askLong(prompt);
    }

    /**
     * Prints {@code prompt} and reads input until it parses as a {@code double}. See
     * {@link ConsoleIO#askDouble(String)}.
     *
     * @param prompt the text to print before reading
     * @return the parsed double
     * @throws NoSuchElementException if input is exhausted
     */
    public static double askDouble(String prompt) {
        return ConsoleIO.system().askDouble(prompt);
    }

    /**
     * Prints {@code prompt} and reads input until it parses as a boolean. See
     * {@link ConsoleIO#askBoolean(String)}.
     *
     * @param prompt the text to print before reading
     * @return the parsed boolean
     * @throws NoSuchElementException if input is exhausted
     */
    public static boolean askBoolean(String prompt) {
        return ConsoleIO.system().askBoolean(prompt);
    }

    /**
     * Asks a yes/no {@code question}. See {@link ConsoleIO#confirm(String)}.
     *
     * @param question the question to ask
     * @return {@code true} if the user answered yes
     * @throws NoSuchElementException if input is exhausted
     */
    public static boolean confirm(String question) {
        return ConsoleIO.system().confirm(question);
    }

    /**
     * Prints a numbered menu of {@code options} and reads a selection. See
     * {@link ConsoleIO#choose(String, List)}.
     *
     * @param prompt the text to print before the option list
     * @param options the choices to present
     * @param <T> the option type
     * @return the selected option
     * @throws IllegalArgumentException if {@code options} is empty
     * @throws NoSuchElementException if input is exhausted
     */
    public static <T> T choose(String prompt, List<T> options) {
        return ConsoleIO.system().choose(prompt, options);
    }

    /**
     * Reads a line without echoing it, where supported. See
     * {@link ConsoleIO#readPassword(String)}.
     *
     * @param prompt the text to print before reading
     * @return the line read
     * @throws NoSuchElementException if input is exhausted
     */
    public static String readPassword(String prompt) {
        return ConsoleIO.system().readPassword(prompt);
    }
}
