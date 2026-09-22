package lithej.core;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class ObjectsXTest {

    @Test
    void orElseReturnsValueWhenNonNull() {
        assertThat(ObjectsX.orElse("a", "b")).isEqualTo("a");
    }

    @Test
    void orElseReturnsFallbackWhenNull() {
        assertThat(ObjectsX.<String>orElse(null, "b")).isEqualTo("b");
    }

    @Test
    void orElseAllowsNullFallback() {
        assertThat(ObjectsX.<String>orElse(null, null)).isNull();
    }

    @Test
    void orElseGetReturnsValueWithoutInvokingSupplier() {
        String result = ObjectsX.orElseGet("a", () -> {
            throw new AssertionError("should not be called");
        });
        assertThat(result).isEqualTo("a");
    }

    @Test
    void orElseGetInvokesSupplierWhenNull() {
        String result = ObjectsX.<String>orElseGet(null, () -> "computed");
        assertThat(result).isEqualTo("computed");
    }

    @Test
    void firstNonNullReturnsFirstPresentValue() {
        assertThat(ObjectsX.firstNonNull(null, null, "c", "d")).isEqualTo("c");
    }

    @Test
    void firstNonNullReturnsNullWhenAllNull() {
        assertThat(ObjectsX.<String>firstNonNull((String) null, null)).isNull();
    }

    @Test
    void firstNonNullReturnsNullForEmptyVarargs() {
        assertThat(ObjectsX.<String>firstNonNull()).isNull();
    }

    @Test
    void isNullAndIsNotNull() {
        assertThat(ObjectsX.isNull(null)).isTrue();
        assertThat(ObjectsX.isNull("x")).isFalse();
        assertThat(ObjectsX.isNotNull(null)).isFalse();
        assertThat(ObjectsX.isNotNull("x")).isTrue();
    }

    @Test
    void toStringOrDefaultUsesValueWhenPresent() {
        assertThat(ObjectsX.toStringOrDefault(42, "none")).isEqualTo("42");
    }

    @Test
    void toStringOrDefaultUsesDefaultWhenNull() {
        assertThat(ObjectsX.toStringOrDefault(null, "none")).isEqualTo("none");
    }
}
