package lithej.console;

import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.PrintStream;
import java.util.List;
import java.util.NoSuchElementException;
import org.junit.jupiter.api.Test;

class ConsoleIOTest {

    private static ConsoleIO consoleWithInput(String scriptedInput) {
        ByteArrayInputStream in = new ByteArrayInputStream(scriptedInput.getBytes(UTF_8));
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        return new ConsoleIO(in, new PrintStream(out, true, UTF_8));
    }

    @Test
    void printAndPrintlnWriteToInjectedStream() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleIO console = new ConsoleIO(new ByteArrayInputStream(new byte[0]), new PrintStream(out, true, UTF_8));

        console.print("no-newline");
        console.println(" and-newline");

        assertThat(out.toString(UTF_8)).isEqualTo("no-newline and-newline" + System.lineSeparator());
    }

    @Test
    void printfWritesFormattedText() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleIO console = new ConsoleIO(new ByteArrayInputStream(new byte[0]), new PrintStream(out, true, UTF_8));

        console.printf("%s is %d", "age", 42);

        assertThat(out.toString(UTF_8)).isEqualTo("age is 42");
    }

    @Test
    void askReadsAndTrimsLine() {
        ConsoleIO console = consoleWithInput("hello world\n");
        assertThat(console.ask("Say something: ")).isEqualTo("hello world");
    }

    @Test
    void askThrowsOnEndOfStream() {
        ConsoleIO console = consoleWithInput("");
        assertThatExceptionOfType(NoSuchElementException.class).isThrownBy(() -> console.ask("? "));
    }

    @Test
    void askWithValidatorRetriesUntilValid() {
        ConsoleIO console = consoleWithInput("bad\nstill-bad\ngood\n");
        String result = console.ask("? ", s -> s.equals("good"), "try again");
        assertThat(result).isEqualTo("good");
    }

    @Test
    void askIntParsesValidIntegerImmediately() {
        ConsoleIO console = consoleWithInput("42\n");
        assertThat(console.askInt("Age: ")).isEqualTo(42);
    }

    @Test
    void askIntRetriesOnInvalidInput() {
        ConsoleIO console = consoleWithInput("not a number\n-7\n");
        assertThat(console.askInt("Age: ")).isEqualTo(-7);
    }

    @Test
    void askIntWithMaxAttemptsThrowsAfterExhausted() {
        ConsoleIO console = consoleWithInput("a\nb\nc\n");
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> console.askInt("? ", 3));
    }

    @Test
    void askIntWithMaxAttemptsSucceedsWithinLimit() {
        ConsoleIO console = consoleWithInput("bad\n5\n");
        assertThat(console.askInt("? ", 3)).isEqualTo(5);
    }

    @Test
    void askLongParsesLargeValues() {
        ConsoleIO console = consoleWithInput("9999999999\n");
        assertThat(console.askLong("? ")).isEqualTo(9999999999L);
    }

    @Test
    void askDoubleRetriesOnInvalidInput() {
        ConsoleIO console = consoleWithInput("nope\n3.14\n");
        assertThat(console.askDouble("? ")).isEqualTo(3.14);
    }

    @Test
    void askBooleanAcceptsVariousAffirmativeAndNegativeForms() {
        assertThat(consoleWithInput("y\n").askBoolean("? ")).isTrue();
        assertThat(consoleWithInput("YES\n").askBoolean("? ")).isTrue();
        assertThat(consoleWithInput("true\n").askBoolean("? ")).isTrue();
        assertThat(consoleWithInput("n\n").askBoolean("? ")).isFalse();
        assertThat(consoleWithInput("No\n").askBoolean("? ")).isFalse();
        assertThat(consoleWithInput("false\n").askBoolean("? ")).isFalse();
    }

    @Test
    void askBooleanRetriesOnUnrecognizedInput() {
        ConsoleIO console = consoleWithInput("maybe\ny\n");
        assertThat(console.askBoolean("? ")).isTrue();
    }

    @Test
    void confirmAppendsYesNoHintToPrompt() {
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ConsoleIO console = new ConsoleIO(
                new ByteArrayInputStream("y\n".getBytes(UTF_8)), new PrintStream(out, true, UTF_8));

        assertThat(console.confirm("Proceed?")).isTrue();
        assertThat(out.toString(UTF_8)).contains("Proceed? (y/n): ");
    }

    @Test
    void chooseReturnsSelectedOption() {
        ConsoleIO console = consoleWithInput("2\n");
        String choice = console.choose("Pick one:", List.of("red", "green", "blue"));
        assertThat(choice).isEqualTo("green");
    }

    @Test
    void chooseRetriesOnOutOfRangeOrNonNumericInput() {
        ConsoleIO console = consoleWithInput("0\nabc\n99\n3\n");
        String choice = console.choose("Pick one:", List.of("red", "green", "blue"));
        assertThat(choice).isEqualTo("blue");
    }

    @Test
    void chooseRejectsEmptyOptionList() {
        ConsoleIO console = consoleWithInput("");
        org.assertj.core.api.Assertions.assertThatIllegalArgumentException()
                .isThrownBy(() -> console.choose("Pick:", List.of()));
    }

    @Test
    void readPasswordFallsBackToPlainReadWhenNoSystemConsole() {
        ConsoleIO console = consoleWithInput("secret\n");
        assertThat(console.readPassword("Password: ")).isEqualTo("secret");
    }

    @Test
    void systemReturnsTheSameSharedInstance() {
        assertThat(ConsoleIO.system()).isSameAs(ConsoleIO.system());
    }

    @Test
    void constructorRejectsNullStreams() {
        org.assertj.core.api.Assertions.assertThatNullPointerException()
                .isThrownBy(() -> new ConsoleIO(null, new PrintStream(new ByteArrayOutputStream())));
        org.assertj.core.api.Assertions.assertThatNullPointerException()
                .isThrownBy(() -> new ConsoleIO(new ByteArrayInputStream(new byte[0]), null));
    }
}
