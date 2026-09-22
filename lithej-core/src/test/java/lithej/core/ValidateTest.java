package lithej.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ValidateTest {

    @Test
    void notNullReturnsValue() {
        assertThat(Validate.notNull("x", "name")).isEqualTo("x");
    }

    @Test
    void notNullThrowsWithFieldNameInMessage() {
        assertThatNullPointerException()
                .isThrownBy(() -> Validate.notNull(null, "userId"))
                .withMessageContaining("userId");
    }

    @Test
    void notBlankAcceptsNonBlankString() {
        assertThat(Validate.notBlank("hello", "name")).isEqualTo("hello");
    }

    @Test
    void notBlankRejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> Validate.notBlank(null, "name"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"", " ", "\t", "\n", "   \t\n  "})
    void notBlankRejectsEmptyOrWhitespace(String value) {
        assertThatIllegalArgumentException().isThrownBy(() -> Validate.notBlank(value, "name"));
    }

    @Test
    void notEmptyCollectionAcceptsNonEmpty() {
        List<String> list = List.of("a");
        assertThat(Validate.notEmpty(list, "list")).isSameAs(list);
    }

    @Test
    void notEmptyCollectionRejectsEmpty() {
        assertThatIllegalArgumentException().isThrownBy(() -> Validate.notEmpty(List.of(), "list"));
    }

    @Test
    void notEmptyCollectionRejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> Validate.notEmpty((List<?>) null, "list"));
    }

    @Test
    void notEmptyMapAcceptsNonEmpty() {
        Map<String, String> map = Map.of("k", "v");
        assertThat(Validate.notEmpty(map, "map")).isSameAs(map);
    }

    @Test
    void notEmptyMapRejectsEmpty() {
        assertThatIllegalArgumentException().isThrownBy(() -> Validate.notEmpty(Map.of(), "map"));
    }

    @Test
    void isTrueAcceptsTrue() {
        Validate.isTrue(true, "should not throw");
    }

    @Test
    void isTrueRejectsFalse() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Validate.isTrue(false, "boom"))
                .withMessage("boom");
    }

    @Test
    void rangeLongAcceptsBoundaryValues() {
        assertThat(Validate.range(0L, 0L, 10L, "n")).isEqualTo(0L);
        assertThat(Validate.range(10L, 0L, 10L, "n")).isEqualTo(10L);
    }

    @Test
    void rangeLongRejectsBelowMinimum() {
        assertThatIllegalArgumentException().isThrownBy(() -> Validate.range(-1L, 0L, 10L, "n"));
    }

    @Test
    void rangeLongRejectsAboveMaximum() {
        assertThatIllegalArgumentException().isThrownBy(() -> Validate.range(11L, 0L, 10L, "n"));
    }

    @Test
    void rangeRejectsInvertedBounds() {
        assertThatIllegalArgumentException().isThrownBy(() -> Validate.range(5L, 10L, 0L, "n"));
    }

    @Test
    void rangeDoubleAcceptsBoundaryValues() {
        assertThat(Validate.range(1.5, 1.5, 2.5, "n")).isEqualTo(1.5);
    }

    @Test
    void positiveAcceptsPositive() {
        assertThat(Validate.positive(1L, "n")).isEqualTo(1L);
    }

    @Test
    void positiveRejectsZeroAndNegative() {
        assertThatIllegalArgumentException().isThrownBy(() -> Validate.positive(0L, "n"));
        assertThatIllegalArgumentException().isThrownBy(() -> Validate.positive(-1L, "n"));
    }

    @Test
    void nonNegativeAcceptsZero() {
        assertThat(Validate.nonNegative(0L, "n")).isEqualTo(0L);
    }

    @Test
    void nonNegativeRejectsNegative() {
        assertThatIllegalArgumentException().isThrownBy(() -> Validate.nonNegative(-1L, "n"));
    }

    @Test
    void matchesAcceptsMatchingValue() {
        Pattern digits = Pattern.compile("\\d+");
        assertThat(Validate.matches("12345", digits, "code")).isEqualTo("12345");
    }

    @Test
    void matchesRejectsNonMatchingValue() {
        Pattern digits = Pattern.compile("\\d+");
        assertThatIllegalArgumentException().isThrownBy(() -> Validate.matches("abc", digits, "code"));
    }

    @Test
    void isEmailAcceptsPlausibleAddresses() {
        assertThat(Validate.isEmail("user@example.com")).isTrue();
        assertThat(Validate.isEmail("first.last+tag@sub.example.co")).isTrue();
    }

    @Test
    void isEmailRejectsImplausibleInputAndNull() {
        assertThat(Validate.isEmail(null)).isFalse();
        assertThat(Validate.isEmail("")).isFalse();
        assertThat(Validate.isEmail("not-an-email")).isFalse();
        assertThat(Validate.isEmail("missing-domain@")).isFalse();
        assertThat(Validate.isEmail("@missing-local.com")).isFalse();
        assertThat(Validate.isEmail("has spaces@example.com")).isFalse();
    }
}
