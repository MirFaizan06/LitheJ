package lithej.core;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.OptionalDouble;
import java.util.OptionalInt;
import java.util.OptionalLong;
import org.junit.jupiter.api.Test;

class NumbersTest {

    @Test
    void parseIntTrimsWhitespace() {
        assertThat(Numbers.parseInt("  42  ")).isEqualTo(42);
    }

    @Test
    void parseIntRejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> Numbers.parseInt(null));
    }

    @Test
    void parseIntRejectsInvalidInput() {
        org.assertj.core.api.Assertions.assertThatExceptionOfType(NumberFormatException.class)
                .isThrownBy(() -> Numbers.parseInt("not a number"));
    }

    @Test
    void parseIntRejectsOverflow() {
        org.assertj.core.api.Assertions.assertThatExceptionOfType(NumberFormatException.class)
                .isThrownBy(() -> Numbers.parseInt("99999999999999999999"));
    }

    @Test
    void tryIntReturnsEmptyForNullBlankOrInvalid() {
        assertThat(Numbers.tryInt(null)).isEqualTo(OptionalInt.empty());
        assertThat(Numbers.tryInt("")).isEqualTo(OptionalInt.empty());
        assertThat(Numbers.tryInt("  ")).isEqualTo(OptionalInt.empty());
        assertThat(Numbers.tryInt("abc")).isEqualTo(OptionalInt.empty());
    }

    @Test
    void tryIntParsesValidInput() {
        assertThat(Numbers.tryInt("-7")).isEqualTo(OptionalInt.of(-7));
    }

    @Test
    void tryLongParsesValidAndInvalidInput() {
        assertThat(Numbers.tryLong("123456789012")).isEqualTo(OptionalLong.of(123456789012L));
        assertThat(Numbers.tryLong("nope")).isEqualTo(OptionalLong.empty());
    }

    @Test
    void parseLongTrimsWhitespace() {
        assertThat(Numbers.parseLong(" 123 ")).isEqualTo(123L);
    }

    @Test
    void tryDoubleParsesValidAndInvalidInput() {
        assertThat(Numbers.tryDouble("3.14")).isEqualTo(OptionalDouble.of(3.14));
        assertThat(Numbers.tryDouble("nope")).isEqualTo(OptionalDouble.empty());
    }

    @Test
    void parseDoubleTrimsWhitespace() {
        assertThat(Numbers.parseDouble(" 2.5 ")).isEqualTo(2.5);
    }

    @Test
    void isIntegerRecognizesValidAndInvalidStrings() {
        assertThat(Numbers.isInteger("42")).isTrue();
        assertThat(Numbers.isInteger("-42")).isTrue();
        assertThat(Numbers.isInteger("4.2")).isFalse();
        assertThat(Numbers.isInteger(null)).isFalse();
    }

    @Test
    void clampIntConstrainsToRange() {
        assertThat(Numbers.clamp(5, 0, 10)).isEqualTo(5);
        assertThat(Numbers.clamp(-5, 0, 10)).isEqualTo(0);
        assertThat(Numbers.clamp(15, 0, 10)).isEqualTo(10);
        assertThat(Numbers.clamp(Integer.MAX_VALUE, 0, 10)).isEqualTo(10);
        assertThat(Numbers.clamp(Integer.MIN_VALUE, 0, 10)).isEqualTo(0);
    }

    @Test
    void clampRejectsInvertedBounds() {
        assertThatIllegalArgumentException().isThrownBy(() -> Numbers.clamp(5, 10, 0));
    }

    @Test
    void clampLongAndDoubleConstrainToRange() {
        assertThat(Numbers.clamp(5L, 0L, 10L)).isEqualTo(5L);
        assertThat(Numbers.clamp(15L, 0L, 10L)).isEqualTo(10L);
        assertThat(Numbers.clamp(1.5, 0.0, 1.0)).isEqualTo(1.0);
        assertThat(Numbers.clamp(-0.5, 0.0, 1.0)).isEqualTo(0.0);
    }

    @Test
    void inRangeChecksBoundaries() {
        assertThat(Numbers.inRange(0, 0, 10)).isTrue();
        assertThat(Numbers.inRange(10, 0, 10)).isTrue();
        assertThat(Numbers.inRange(11, 0, 10)).isFalse();
        assertThat(Numbers.inRange(-1, 0, 10)).isFalse();
    }

    @Test
    void roundUsesHalfUpNotBinaryFloatingPointRounding() {
        assertThat(Numbers.round(2.675, 2)).isEqualTo(2.68);
        assertThat(Numbers.round(1.005, 2)).isEqualTo(1.01);
        assertThat(Numbers.round(1.2345, 0)).isEqualTo(1.0);
    }

    @Test
    void roundRejectsNonFiniteValues() {
        assertThatIllegalArgumentException().isThrownBy(() -> Numbers.round(Double.NaN, 2));
        assertThatIllegalArgumentException().isThrownBy(() -> Numbers.round(Double.POSITIVE_INFINITY, 2));
    }

    @Test
    void roundRejectsNegativeDecimalPlaces() {
        assertThatIllegalArgumentException().isThrownBy(() -> Numbers.round(1.0, -1));
    }
}
