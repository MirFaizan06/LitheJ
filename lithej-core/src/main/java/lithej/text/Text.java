package lithej.text;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * String helpers for the operations that come up repeatedly in application code: blank
 * checks, splitting, joining, padding, casing, and simple substring extraction.
 *
 * <p>These methods operate on Unicode code points where that matters (e.g.
 * {@link #capitalize(String)} uses {@link Character#toTitleCase(int)} on the first code
 * point rather than the first {@code char}), but they do <b>not</b> implement full
 * Unicode grapheme-cluster segmentation (UAX #29). {@link #reverse(String)}, for
 * example, correctly preserves surrogate pairs but will still split a base character
 * from its combining marks. If you need grapheme-correct text processing, use
 * {@link java.text.BreakIterator}.
 *
 * <p><b>Null policy:</b> unless documented otherwise, a {@code null} {@link CharSequence}
 * argument is treated as absent input, not an error: {@link #isBlank(CharSequence)}
 * returns {@code true} for {@code null}, and methods returning a collection return an
 * empty collection for {@code null} input. Methods that return a single {@link String}
 * derived from the input (e.g. {@link #capitalize(String)}) throw
 * {@link NullPointerException} on {@code null}, since there is no reasonable non-null
 * default to return.
 *
 * <p><b>Thread safety:</b> this class is stateless and thread-safe.
 */
public final class Text {

    private static final String ELLIPSIS = "...";
    private static final int ELLIPSIS_LENGTH = ELLIPSIS.length();

    private Text() {
    }

    /**
     * Returns {@code true} if {@code value} is {@code null}, empty, or contains only
     * whitespace.
     *
     * @param value the value to check, may be {@code null}
     * @return {@code true} if {@code value} is null or blank
     */
    public static boolean isBlank(CharSequence value) {
        return value == null || value.toString().isBlank();
    }

    /**
     * Returns {@code true} if {@code value} is non-null and contains at least one
     * non-whitespace character.
     *
     * @param value the value to check, may be {@code null}
     * @return {@code true} if {@code value} is not null and not blank
     */
    public static boolean isNotBlank(CharSequence value) {
        return !isBlank(value);
    }

    /**
     * Splits {@code value} into whitespace-separated words, discarding empty tokens.
     *
     * @param value the string to split, may be {@code null}
     * @return the words in {@code value}, in order; an empty list if {@code value} is
     *     {@code null} or blank
     */
    public static List<String> words(String value) {
        if (isBlank(value)) {
            return List.of();
        }
        return Arrays.stream(value.trim().split("\\s+")).collect(Collectors.toList());
    }

    /**
     * Splits {@code value} into lines, recognizing {@code \n}, {@code \r}, and
     * {@code \r\n} as line terminators (via {@link String#lines()}).
     *
     * @param value the string to split, may be {@code null}
     * @return the lines in {@code value}, without terminators; an empty list if
     *     {@code value} is {@code null} or empty
     */
    public static List<String> lines(String value) {
        if (value == null || value.isEmpty()) {
            return List.of();
        }
        return value.lines().collect(Collectors.toList());
    }

    /**
     * Joins {@code parts} with {@code delimiter} between each element.
     *
     * @param delimiter the separator to insert between elements
     * @param parts the elements to join, may be {@code null}
     * @return the joined string; an empty string if {@code parts} is {@code null} or
     *     empty
     */
    public static String join(CharSequence delimiter, Iterable<? extends CharSequence> parts) {
        if (parts == null) {
            return "";
        }
        StringBuilder result = new StringBuilder();
        boolean first = true;
        for (CharSequence part : parts) {
            if (!first) {
                result.append(delimiter);
            }
            result.append(part);
            first = false;
        }
        return result.toString();
    }

    /**
     * Repeats {@code value} {@code count} times, with no separator.
     *
     * @param value the string to repeat
     * @param count the number of repetitions; {@code 0} yields an empty string
     * @return {@code value} repeated {@code count} times
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code count} is negative
     */
    public static String repeat(String value, int count) {
        if (count < 0) {
            throw new IllegalArgumentException("count must not be negative, was " + count);
        }
        return value.repeat(count);
    }

    /**
     * Reverses the code points of {@code value}.
     *
     * <p>Surrogate pairs (characters outside the Basic Multilingual Plane) are kept
     * intact rather than being split, but combining marks (e.g. accents applied as a
     * separate code point) will be reordered relative to their base character. Use
     * {@link java.text.BreakIterator} for grapheme-correct reversal.
     *
     * @param value the string to reverse
     * @return {@code value} with its code points in reverse order
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public static String reverse(String value) {
        int[] codePoints = value.codePoints().toArray();
        StringBuilder result = new StringBuilder(value.length());
        for (int i = codePoints.length - 1; i >= 0; i--) {
            result.appendCodePoint(codePoints[i]);
        }
        return result.toString();
    }

    /**
     * Converts the first code point of {@code value} to title case, leaving the rest
     * unchanged.
     *
     * @param value the string to capitalize
     * @return {@code value} with its first code point title-cased; {@code value}
     *     unchanged if empty
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public static String capitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        int first = value.codePointAt(0);
        String head = new String(Character.toChars(Character.toTitleCase(first)));
        return head + value.substring(Character.charCount(first));
    }

    /**
     * Converts the first code point of {@code value} to lower case, leaving the rest
     * unchanged.
     *
     * @param value the string to uncapitalize
     * @return {@code value} with its first code point lower-cased; {@code value}
     *     unchanged if empty
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public static String uncapitalize(String value) {
        if (value.isEmpty()) {
            return value;
        }
        int first = value.codePointAt(0);
        String head = new String(Character.toChars(Character.toLowerCase(first)));
        return head + value.substring(Character.charCount(first));
    }

    /**
     * Capitalizes the first letter of every whitespace-separated word in {@code value},
     * lower-casing the remainder of each word.
     *
     * @param value the string to convert
     * @return the title-cased string
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public static String titleCase(String value) {
        if (value.isBlank()) {
            return value;
        }
        String[] words = value.split(" ", -1);
        StringBuilder result = new StringBuilder(value.length());
        for (int i = 0; i < words.length; i++) {
            if (i > 0) {
                result.append(' ');
            }
            String word = words[i];
            if (!word.isEmpty()) {
                result.append(capitalize(word.toLowerCase(Locale.ROOT)));
            }
        }
        return result.toString();
    }

    /**
     * Truncates {@code value} to at most {@code maxLength} characters.
     *
     * @param value the string to truncate
     * @param maxLength the maximum number of {@code char}s to keep; must be
     *     {@code >= 0}
     * @return {@code value} unchanged if it already fits, otherwise its first
     *     {@code maxLength} characters
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code maxLength} is negative
     */
    public static String truncate(String value, int maxLength) {
        if (maxLength < 0) {
            throw new IllegalArgumentException("maxLength must not be negative, was " + maxLength);
        }
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    /**
     * Truncates {@code value} to at most {@code maxLength} characters, replacing the
     * last three characters of a truncated string with an ellipsis ({@code "..."}) so
     * the result never exceeds {@code maxLength}.
     *
     * @param value the string to truncate
     * @param maxLength the maximum length of the result; must be {@code >= 3}, since
     *     the ellipsis itself is 3 characters
     * @return {@code value} unchanged if it already fits, otherwise a truncated string
     *     ending in {@code "..."}
     * @throws NullPointerException if {@code value} is {@code null}
     * @throws IllegalArgumentException if {@code maxLength} is less than {@code 3}
     */
    public static String ellipsis(String value, int maxLength) {
        if (maxLength < ELLIPSIS_LENGTH) {
            throw new IllegalArgumentException("maxLength must be at least 3, was " + maxLength);
        }
        if (value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength - ELLIPSIS_LENGTH) + ELLIPSIS;
    }

    /**
     * Pads {@code value} on the left with {@code padChar} until it reaches
     * {@code length} characters.
     *
     * @param value the string to pad
     * @param length the target length
     * @param padChar the character to pad with
     * @return {@code value} unchanged if it already meets {@code length}, otherwise
     *     left-padded to {@code length}
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public static String padLeft(String value, int length, char padChar) {
        int deficit = length - value.length();
        return deficit <= 0 ? value : String.valueOf(padChar).repeat(deficit) + value;
    }

    /**
     * Pads {@code value} on the right with {@code padChar} until it reaches
     * {@code length} characters.
     *
     * @param value the string to pad
     * @param length the target length
     * @param padChar the character to pad with
     * @return {@code value} unchanged if it already meets {@code length}, otherwise
     *     right-padded to {@code length}
     * @throws NullPointerException if {@code value} is {@code null}
     */
    public static String padRight(String value, int length, char padChar) {
        int deficit = length - value.length();
        return deficit <= 0 ? value : value + String.valueOf(padChar).repeat(deficit);
    }

    /**
     * Removes {@code prefix} from the start of {@code value}, if present.
     *
     * @param value the string to strip
     * @param prefix the prefix to remove
     * @return {@code value} without its leading {@code prefix}, or {@code value}
     *     unchanged if it does not start with {@code prefix}
     * @throws NullPointerException if {@code value} or {@code prefix} is {@code null}
     */
    public static String removePrefix(String value, String prefix) {
        return value.startsWith(prefix) ? value.substring(prefix.length()) : value;
    }

    /**
     * Removes {@code suffix} from the end of {@code value}, if present.
     *
     * @param value the string to strip
     * @param suffix the suffix to remove
     * @return {@code value} without its trailing {@code suffix}, or {@code value}
     *     unchanged if it does not end with {@code suffix}
     * @throws NullPointerException if {@code value} or {@code suffix} is {@code null}
     */
    public static String removeSuffix(String value, String suffix) {
        return value.endsWith(suffix) ? value.substring(0, value.length() - suffix.length()) : value;
    }

    /**
     * Returns the portion of {@code value} before the first occurrence of
     * {@code separator}.
     *
     * @param value the string to search
     * @param separator the separator to search for
     * @return the substring before {@code separator}, or all of {@code value} if
     *     {@code separator} does not occur
     * @throws NullPointerException if {@code value} or {@code separator} is {@code null}
     */
    public static String substringBefore(String value, String separator) {
        int index = value.indexOf(separator);
        return index < 0 ? value : value.substring(0, index);
    }

    /**
     * Returns the portion of {@code value} after the first occurrence of
     * {@code separator}.
     *
     * @param value the string to search
     * @param separator the separator to search for
     * @return the substring after {@code separator}, or an empty string if
     *     {@code separator} does not occur
     * @throws NullPointerException if {@code value} or {@code separator} is {@code null}
     */
    public static String substringAfter(String value, String separator) {
        int index = value.indexOf(separator);
        return index < 0 ? "" : value.substring(index + separator.length());
    }

    /**
     * Returns the substring strictly between the first occurrence of {@code open} and
     * the first occurrence of {@code close} that follows it.
     *
     * @param value the string to search
     * @param open the opening delimiter
     * @param close the closing delimiter
     * @return the text between {@code open} and {@code close}, or {@link Optional#empty()}
     *     if either delimiter is missing
     * @throws NullPointerException if any argument is {@code null}
     */
    public static Optional<String> between(String value, String open, String close) {
        int start = value.indexOf(open);
        if (start < 0) {
            return Optional.empty();
        }
        start += open.length();
        int end = value.indexOf(close, start);
        if (end < 0) {
            return Optional.empty();
        }
        return Optional.of(value.substring(start, end));
    }

    /**
     * Counts the non-overlapping occurrences of {@code needle} in {@code haystack}.
     *
     * @param haystack the string to search
     * @param needle the substring to count; must not be empty
     * @return the number of non-overlapping occurrences of {@code needle}
     * @throws NullPointerException if either argument is {@code null}
     * @throws IllegalArgumentException if {@code needle} is empty
     */
    public static int count(String haystack, String needle) {
        if (needle.isEmpty()) {
            throw new IllegalArgumentException("needle must not be empty");
        }
        int count = 0;
        int index = haystack.indexOf(needle);
        while (index >= 0) {
            count++;
            index = haystack.indexOf(needle, index + needle.length());
        }
        return count;
    }
}
