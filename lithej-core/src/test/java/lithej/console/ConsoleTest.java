package lithej.console;

import static org.assertj.core.api.Assertions.assertThatCode;

import org.junit.jupiter.api.Test;

/**
 * {@link Console} is a thin static facade with no logic of its own: every method is a
 * single-line delegation to {@link ConsoleIO#system()}. The read/write/retry behavior
 * it delegates to is exhaustively covered against injected streams in
 * {@link ConsoleIOTest}.
 *
 * <p>These tests deliberately do <b>not</b> attempt to redirect {@code System.in} to
 * exercise the {@code ask*} delegations: {@link ConsoleIO#system()} wraps the real
 * {@code System.in} in a single {@link java.io.BufferedReader} created once, on first
 * use, for the life of the JVM (documented on {@link ConsoleIO#system()}), so swapping
 * {@code System.in} afterward would not reach it. That is precisely why application
 * code should depend on {@link ConsoleIO} — constructed over injected streams — rather
 * than {@link Console} wherever it needs to be testable.
 */
class ConsoleTest {

    @Test
    void printMethodsDoNotThrow() {
        assertThatCode(() -> {
            Console.print("x");
            Console.println("y");
            Console.println();
            Console.printf("%d", 1);
        }).doesNotThrowAnyException();
    }
}
