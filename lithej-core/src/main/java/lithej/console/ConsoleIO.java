package lithej.console;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.PrintStream;
import java.io.UncheckedIOException;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.OptionalInt;
import java.util.function.Predicate;
import lithej.core.Numbers;

/**
 * A console bound to a specific input and output stream.
 *
 * <p>{@link Console} is a convenient static facade over a shared instance bound to
 * {@link System#in} and {@link System#out}, suitable for scripts and small CLI tools.
 * {@code ConsoleIO} is the class to use when you need to <b>test</b> console
 * interaction: construct one over an {@link java.io.ByteArrayInputStream} holding
 * scripted input and a {@link java.io.ByteArrayOutputStream}-backed
 * {@link PrintStream}, call methods on it directly, and assert on the captured output.
 * No global state is touched.
 *
 * <pre>{@code
 * var in = new ByteArrayInputStream("42\n".getBytes(UTF_8));
 * var out = new ByteArrayOutputStream();
 * ConsoleIO console = new ConsoleIO(in, new PrintStream(out, true, UTF_8));
 *
 * int age = console.askInt("Enter age: ");
 * assertEquals(42, age);
 * }</pre>
 *
 * <p><b>Thread safety:</b> not thread-safe. A {@code ConsoleIO} reads from a single
 * buffered stream; concurrent calls from multiple threads can interleave and corrupt
 * input. Use one {@code ConsoleIO} per logical console session, from one thread.
 */
public final class ConsoleIO {

    private final BufferedReader reader;
    private final PrintStream out;

    /**
     * Creates a console over the given streams, decoding input as UTF-8.
     *
     * @param in the input stream to read lines from
     * @param out the stream to write prompts and output to
     * @throws NullPointerException if either argument is {@code null}
     */
    public ConsoleIO(InputStream in, PrintStream out) {
        this(in, out, StandardCharsets.UTF_8);
    }

    /**
     * Creates a console over the given streams, decoding input using {@code charset}.
     *
     * @param in the input stream to read lines from
     * @param out the stream to write prompts and output to
     * @param charset the charset used to decode {@code in}
     * @throws NullPointerException if any argument is {@code null}
     */
    public ConsoleIO(InputStream in, PrintStream out, Charset charset) {
        Objects.requireNonNull(in, "in must not be null");
        this.out = Objects.requireNonNull(out, "out must not be null");
        Objects.requireNonNull(charset, "charset must not be null");
        this.reader = new BufferedReader(new InputStreamReader(in, charset));
    }

    /**
     * Returns a {@code ConsoleIO} bound to {@link System#in} and {@link System#out}.
     *
     * <p>The returned instance wraps {@code System.in} in a single, shared
     * {@link BufferedReader} for the life of the JVM, matching the standard pattern for
     * reading console input in Java: creating a fresh reader per call would silently
     * discard any input already buffered from a previous read. Because of this shared
     * buffer, this instance should be read from one thread at a time.
     *
     * @return the shared system console
     */
    public static ConsoleIO system() {
        return SystemHolder.INSTANCE;
    }

    /**
     * Writes {@code text} with no trailing newline.
     *
     * @param text the text to write
     */
    public void print(Object text) {
        out.print(text);
        out.flush();
    }

    /**
     * Writes {@code text} followed by a newline.
     *
     * @param text the text to write
     */
    public void println(Object text) {
        out.println(text);
    }

    /**
     * Writes a blank line.
     */
    public void println() {
        out.println();
    }

    /**
     * Writes a formatted string, as per {@link String#format(String, Object...)}.
     *
     * @param format the format string
     * @param args the format arguments
     */
    public void printf(String format, Object... args) {
        out.printf(format, args);
        out.flush();
    }

    /**
     * Prints {@code prompt} and reads a single line of input.
     *
     * @param prompt the text to print before reading, with no trailing newline
     * @return the line read, without its line terminator
     * @throws NoSuchElementException if the input stream is exhausted (end of file)
     *     before a line is read
     */
    public String ask(String prompt) {
        print(prompt);
        return readLineOrThrow();
    }

    /**
     * Prints {@code prompt} and reads input repeatedly until it satisfies
     * {@code validator}, re-prompting with {@code errorMessage} on each invalid
     * attempt.
     *
     * @param prompt the text to print before reading
     * @param validator predicate the input must satisfy
     * @param errorMessage message printed (on its own line) after invalid input
     * @return the first line of input that satisfies {@code validator}
     * @throws NoSuchElementException if the input stream is exhausted before valid
     *     input is read
     */
    public String ask(String prompt, Predicate<String> validator, String errorMessage) {
        while (true) {
            String line = ask(prompt);
            if (validator.test(line)) {
                return line;
            }
            println(errorMessage);
        }
    }

    /**
     * Prints {@code prompt} and reads input until it parses as an {@code int},
     * re-prompting on invalid input. Retries without limit until valid input is given
     * or the stream ends.
     *
     * @param prompt the text to print before reading
     * @return the parsed integer
     * @throws NoSuchElementException if the input stream is exhausted before valid
     *     input is read
     */
    public int askInt(String prompt) {
        return askInt(prompt, -1);
    }

    /**
     * Prints {@code prompt} and reads input until it parses as an {@code int},
     * re-prompting on invalid input, giving up after {@code maxAttempts} invalid
     * entries.
     *
     * @param prompt the text to print before reading
     * @param maxAttempts the maximum number of attempts, or a negative number for no
     *     limit
     * @return the parsed integer
     * @throws NoSuchElementException if the input stream is exhausted before valid
     *     input is read
     * @throws IllegalStateException if {@code maxAttempts} is reached without valid
     *     input
     */
    public int askInt(String prompt, int maxAttempts) {
        int attempt = 0;
        while (maxAttempts < 0 || attempt < maxAttempts) {
            attempt++;
            String line = ask(prompt);
            try {
                return Integer.parseInt(line.trim());
            } catch (NumberFormatException e) {
                println("Please enter a whole number.");
            }
        }
        throw new IllegalStateException("No valid integer entered after " + maxAttempts + " attempts");
    }

    /**
     * Prints {@code prompt} and reads input until it parses as a {@code long},
     * re-prompting on invalid input, without limit.
     *
     * @param prompt the text to print before reading
     * @return the parsed long
     * @throws NoSuchElementException if the input stream is exhausted before valid
     *     input is read
     */
    public long askLong(String prompt) {
        while (true) {
            String line = ask(prompt);
            try {
                return Long.parseLong(line.trim());
            } catch (NumberFormatException e) {
                println("Please enter a whole number.");
            }
        }
    }

    /**
     * Prints {@code prompt} and reads input until it parses as a {@code double},
     * re-prompting on invalid input, without limit.
     *
     * @param prompt the text to print before reading
     * @return the parsed double
     * @throws NoSuchElementException if the input stream is exhausted before valid
     *     input is read
     */
    public double askDouble(String prompt) {
        while (true) {
            String line = ask(prompt);
            try {
                return Double.parseDouble(line.trim());
            } catch (NumberFormatException e) {
                println("Please enter a number.");
            }
        }
    }

    /**
     * Prints {@code prompt} and reads input until it is recognized as a boolean
     * ({@code y}/{@code yes}/{@code true} for {@code true}; {@code n}/{@code no}/
     * {@code false} for {@code false}, case-insensitive), re-prompting on invalid
     * input.
     *
     * @param prompt the text to print before reading
     * @return the parsed boolean
     * @throws NoSuchElementException if the input stream is exhausted before valid
     *     input is read
     */
    public boolean askBoolean(String prompt) {
        while (true) {
            String line = ask(prompt).trim().toLowerCase(Locale.ROOT);
            switch (line) {
                case "y":
                case "yes":
                case "true":
                    return true;
                case "n":
                case "no":
                case "false":
                    return false;
                default:
                    println("Please answer yes or no.");
            }
        }
    }

    /**
     * Prints {@code question} followed by {@code " (y/n): "} and reads a yes/no
     * answer. Equivalent to {@code askBoolean(question + " (y/n): ")}.
     *
     * @param question the question to ask
     * @return {@code true} if the user answered yes
     * @throws NoSuchElementException if the input stream is exhausted before valid
     *     input is read
     */
    public boolean confirm(String question) {
        return askBoolean(question + " (y/n): ");
    }

    /**
     * Prints {@code prompt} followed by a numbered list of {@code options}, then reads
     * a 1-based selection, re-prompting on invalid input.
     *
     * @param prompt the text to print before the option list
     * @param options the choices to present, in display order
     * @param <T> the option type
     * @return the selected option
     * @throws IllegalArgumentException if {@code options} is empty
     * @throws NoSuchElementException if the input stream is exhausted before a valid
     *     selection is read
     */
    public <T> T choose(String prompt, List<T> options) {
        if (options.isEmpty()) {
            throw new IllegalArgumentException("options must not be empty");
        }
        println(prompt);
        for (int i = 0; i < options.size(); i++) {
            println("  " + (i + 1) + ") " + options.get(i));
        }
        while (true) {
            String line = ask("Enter a number 1-" + options.size() + ": ").trim();
            OptionalInt index = Numbers.tryInt(line);
            if (index.isPresent() && index.getAsInt() >= 1 && index.getAsInt() <= options.size()) {
                return options.get(index.getAsInt() - 1);
            }
            println("Please enter a number between 1 and " + options.size() + ".");
        }
    }

    /**
     * Prints {@code prompt} and reads a line without echoing it to the terminal, using
     * {@link System#console()} when this instance is {@link #system()} and a real
     * console is attached.
     *
     * <p>When this instance was constructed over custom streams (as in tests), or no
     * real console is attached (e.g. output is redirected to a file or an IDE run
     * console), there is no JDK API to suppress echo on an arbitrary stream, so this
     * method falls back to a normal, visible read. Do not rely on masking when
     * {@code stdout}/{@code stdin} may be redirected.
     *
     * @param prompt the text to print before reading
     * @return the line read, without its line terminator
     * @throws NoSuchElementException if the input stream is exhausted before a line is
     *     read
     */
    public String readPassword(String prompt) {
        String masked = readFromSystemConsole(prompt);
        return masked != null ? masked : ask(prompt);
    }

    private String readFromSystemConsole(String prompt) {
        if (this != SystemHolder.INSTANCE) {
            return null;
        }
        java.io.Console systemConsole = System.console();
        if (systemConsole == null) {
            return null;
        }
        char[] chars = systemConsole.readPassword("%s", prompt);
        if (chars == null) {
            throw new NoSuchElementException("No input available");
        }
        return new String(chars);
    }

    private String readLineOrThrow() {
        try {
            String line = reader.readLine();
            if (line == null) {
                throw new NoSuchElementException("No input available (end of stream)");
            }
            return line;
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    private static final class SystemHolder {
        static final ConsoleIO INSTANCE = new ConsoleIO(System.in, System.out);

        private SystemHolder() {
        }
    }
}
