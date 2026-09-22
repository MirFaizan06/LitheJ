package lithej.text;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;

class TextTest {

    @Test
    void isBlankRecognizesNullEmptyAndWhitespace() {
        assertThat(Text.isBlank(null)).isTrue();
        assertThat(Text.isBlank("")).isTrue();
        assertThat(Text.isBlank("   ")).isTrue();
        assertThat(Text.isBlank("\t\n")).isTrue();
        assertThat(Text.isBlank("a")).isFalse();
    }

    @Test
    void isNotBlankIsInverseOfIsBlank() {
        assertThat(Text.isNotBlank(null)).isFalse();
        assertThat(Text.isNotBlank("x")).isTrue();
    }

    @Test
    void wordsSplitsOnWhitespaceAndDropsEmptyTokens() {
        assertThat(Text.words("  hello   world  ")).containsExactly("hello", "world");
        assertThat(Text.words("")).isEmpty();
        assertThat(Text.words(null)).isEmpty();
        assertThat(Text.words("single")).containsExactly("single");
    }

    @Test
    void linesSplitsOnVariousLineTerminators() {
        assertThat(Text.lines("a\nb\r\nc\rd")).containsExactly("a", "b", "c", "d");
        assertThat(Text.lines("")).isEmpty();
        assertThat(Text.lines(null)).isEmpty();
    }

    @Test
    void joinInsertsDelimiterBetweenElements() {
        assertThat(Text.join(", ", List.of("a", "b", "c"))).isEqualTo("a, b, c");
        assertThat(Text.join(", ", List.of("only"))).isEqualTo("only");
        assertThat(Text.join(", ", List.of())).isEqualTo("");
        assertThat(Text.join(", ", null)).isEqualTo("");
    }

    @Test
    void repeatRepeatsStringNTimes() {
        assertThat(Text.repeat("ab", 3)).isEqualTo("ababab");
        assertThat(Text.repeat("ab", 0)).isEqualTo("");
    }

    @Test
    void repeatRejectsNegativeCount() {
        assertThatIllegalArgumentException().isThrownBy(() -> Text.repeat("a", -1));
    }

    @Test
    void reverseReversesCodePointsAndKeepsSurrogatePairsIntact() {
        assertThat(Text.reverse("abc")).isEqualTo("cba");
        assertThat(Text.reverse("")).isEqualTo("");
        // U+1F600 (grinning face) is a surrogate pair; it must survive reversal intact.
        String emoji = "a😀b";
        assertThat(Text.reverse(emoji)).isEqualTo("b😀a");
    }

    @Test
    void reverseRejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> Text.reverse(null));
    }

    @Test
    void capitalizeUppercasesFirstCodePointOnly() {
        assertThat(Text.capitalize("hello")).isEqualTo("Hello");
        assertThat(Text.capitalize("Hello")).isEqualTo("Hello");
        assertThat(Text.capitalize("")).isEqualTo("");
    }

    @Test
    void uncapitalizeLowercasesFirstCodePointOnly() {
        assertThat(Text.uncapitalize("Hello")).isEqualTo("hello");
        assertThat(Text.uncapitalize("")).isEqualTo("");
    }

    @Test
    void titleCaseCapitalizesEachWord() {
        assertThat(Text.titleCase("hello world")).isEqualTo("Hello World");
        assertThat(Text.titleCase("HELLO WORLD")).isEqualTo("Hello World");
        assertThat(Text.titleCase("")).isEqualTo("");
    }

    @Test
    void truncateShortensLongStringsAndLeavesShortOnesAlone() {
        assertThat(Text.truncate("hello world", 5)).isEqualTo("hello");
        assertThat(Text.truncate("hi", 5)).isEqualTo("hi");
        assertThat(Text.truncate("hi", 0)).isEqualTo("");
    }

    @Test
    void truncateRejectsNegativeMaxLength() {
        assertThatIllegalArgumentException().isThrownBy(() -> Text.truncate("hi", -1));
    }

    @Test
    void ellipsisAddsEllipsisOnlyWhenTruncated() {
        assertThat(Text.ellipsis("hello world", 8)).isEqualTo("hello...");
        assertThat(Text.ellipsis("hi", 8)).isEqualTo("hi");
        assertThat(Text.ellipsis("hello", 5)).isEqualTo("hello");
    }

    @Test
    void ellipsisRejectsMaxLengthBelowThree() {
        assertThatIllegalArgumentException().isThrownBy(() -> Text.ellipsis("hello", 2));
    }

    @Test
    void padLeftAndPadRightPadToLength() {
        assertThat(Text.padLeft("7", 3, '0')).isEqualTo("007");
        assertThat(Text.padRight("7", 3, '0')).isEqualTo("700");
        assertThat(Text.padLeft("already-long", 3, '0')).isEqualTo("already-long");
    }

    @Test
    void removePrefixAndRemoveSuffixStripWhenPresent() {
        assertThat(Text.removePrefix("prefixed-value", "prefixed-")).isEqualTo("value");
        assertThat(Text.removePrefix("value", "prefixed-")).isEqualTo("value");
        assertThat(Text.removeSuffix("value.txt", ".txt")).isEqualTo("value");
        assertThat(Text.removeSuffix("value", ".txt")).isEqualTo("value");
    }

    @Test
    void substringBeforeAndAfterSplitOnFirstOccurrence() {
        assertThat(Text.substringBefore("a=b=c", "=")).isEqualTo("a");
        assertThat(Text.substringAfter("a=b=c", "=")).isEqualTo("b=c");
        assertThat(Text.substringBefore("no-separator", "=")).isEqualTo("no-separator");
        assertThat(Text.substringAfter("no-separator", "=")).isEqualTo("");
    }

    @Test
    void betweenExtractsTextBetweenDelimiters() {
        assertThat(Text.between("<a>value</a>", "<a>", "</a>")).isEqualTo(Optional.of("value"));
        assertThat(Text.between("no delimiters here", "<a>", "</a>")).isEqualTo(Optional.empty());
        assertThat(Text.between("<a>unterminated", "<a>", "</a>")).isEqualTo(Optional.empty());
    }

    @Test
    void countCountsNonOverlappingOccurrences() {
        assertThat(Text.count("aaaa", "aa")).isEqualTo(2);
        assertThat(Text.count("abcabc", "abc")).isEqualTo(2);
        assertThat(Text.count("abc", "z")).isEqualTo(0);
    }

    @Test
    void countRejectsEmptyNeedle() {
        assertThatIllegalArgumentException().isThrownBy(() -> Text.count("abc", ""));
    }
}
