package lithej.examples.console;

import java.util.List;
import lithej.console.Console;

/**
 * Demonstrates {@link Console}: prompted input with automatic retry on invalid
 * numbers, a yes/no confirmation, and a numbered choice menu.
 */
public final class ConsoleExample {

    private ConsoleExample() {
    }

    public static void main(String[] args) {
        String name = Console.ask("What's your name? ");
        int age = Console.askInt("How old are you? ");

        Console.println("Hello, " + name + "! You are " + age + " years old.");

        String favoriteColor = Console.choose(
                "Pick your favorite color:", List.of("red", "green", "blue", "purple"));
        Console.println("Nice choice: " + favoriteColor);

        boolean proceed = Console.confirm("Print a summary?");
        if (proceed) {
            Console.printf("%s (%d), favorite color: %s%n", name, age, favoriteColor);
        } else {
            Console.println("Okay, skipping the summary.");
        }
    }
}
