package lithej.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalInt;
import org.junit.jupiter.api.Test;

/**
 * {@link Env} reads the real process environment, which a test cannot set. These
 * tests use one variable that is present on effectively every platform LitheJ targets
 * ({@code PATH} on Windows, or {@code PATH}/{@code HOME} on Unix-likes — we probe for
 * whichever is set rather than hardcoding one) to exercise the "present" path, and a
 * deliberately implausible name to exercise the "absent" path deterministically.
 */
class EnvTest {

    private static final String DEFINITELY_UNSET = "LITHEJ_TEST_VAR_THAT_DOES_NOT_EXIST_98765";

    private static String presentVariableName() {
        if (System.getenv("PATH") != null) {
            return "PATH";
        }
        if (System.getenv("Path") != null) {
            return "Path";
        }
        return System.getenv().keySet().iterator().next();
    }

    @Test
    void getReturnsValueForSetVariable() {
        String name = presentVariableName();
        assertThat(Env.get(name)).isEqualTo(Optional.of(System.getenv(name)));
    }

    @Test
    void getReturnsEmptyForUnsetVariable() {
        assertThat(Env.get(DEFINITELY_UNSET)).isEmpty();
    }

    @Test
    void getRejectsNullName() {
        assertThatNullPointerException().isThrownBy(() -> Env.get(null));
    }

    @Test
    void getOrDefaultUsesDefaultForUnsetVariable() {
        assertThat(Env.getOrDefault(DEFINITELY_UNSET, "fallback")).isEqualTo("fallback");
    }

    @Test
    void requireReturnsValueForSetVariable() {
        String name = presentVariableName();
        assertThat(Env.require(name)).isEqualTo(System.getenv(name));
    }

    @Test
    void requireThrowsForUnsetVariable() {
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> Env.require(DEFINITELY_UNSET))
                .withMessageContaining(DEFINITELY_UNSET);
    }

    @Test
    void getIntReturnsEmptyForUnsetOrNonNumericVariable() {
        assertThat(Env.getInt(DEFINITELY_UNSET)).isEqualTo(OptionalInt.empty());
        // PATH is present but not a valid int: exercises the "present, unparseable" path.
        assertThat(Env.getInt(presentVariableName())).isEqualTo(OptionalInt.empty());
    }

    @Test
    void getLongReturnsEmptyForPresentNonNumericVariable() {
        assertThat(Env.getLong(presentVariableName())).isEqualTo(java.util.OptionalLong.empty());
    }

    @Test
    void parseBooleanRecognizesTrueFalseAndUnrecognizedForms() {
        assertThat(Env.parseBoolean("true")).contains(Boolean.TRUE);
        assertThat(Env.parseBoolean("TRUE")).contains(Boolean.TRUE);
        assertThat(Env.parseBoolean("1")).contains(Boolean.TRUE);
        assertThat(Env.parseBoolean("yes")).contains(Boolean.TRUE);
        assertThat(Env.parseBoolean("y")).contains(Boolean.TRUE);
        assertThat(Env.parseBoolean("false")).contains(Boolean.FALSE);
        assertThat(Env.parseBoolean("0")).contains(Boolean.FALSE);
        assertThat(Env.parseBoolean("no")).contains(Boolean.FALSE);
        assertThat(Env.parseBoolean("n")).contains(Boolean.FALSE);
        assertThat(Env.parseBoolean("maybe")).isEmpty();
        assertThat(Env.parseBoolean("  TRUE  ")).contains(Boolean.TRUE);
    }

    @Test
    void getIntWithDefaultUsesDefaultForUnsetVariable() {
        assertThat(Env.getInt(DEFINITELY_UNSET, 42)).isEqualTo(42);
    }

    @Test
    void getLongReturnsEmptyForUnsetVariable() {
        assertThat(Env.getLong(DEFINITELY_UNSET)).isEqualTo(java.util.OptionalLong.empty());
    }

    @Test
    void getBooleanReturnsEmptyForUnsetVariable() {
        assertThat(Env.getBoolean(DEFINITELY_UNSET)).isEmpty();
        // PATH is present but not a recognized boolean form: exercises the
        // "present, unrecognized" path through Optional#flatMap.
        assertThat(Env.getBoolean(presentVariableName())).isEmpty();
    }

    @Test
    void getBooleanWithDefaultUsesDefaultForUnsetVariable() {
        assertThat(Env.getBoolean(DEFINITELY_UNSET, true)).isTrue();
        assertThat(Env.getBoolean(DEFINITELY_UNSET, false)).isFalse();
    }

    @Test
    void allMethodsRejectNullName() {
        assertThatNullPointerException().isThrownBy(() -> Env.getOrDefault(null, "x"));
        assertThatNullPointerException().isThrownBy(() -> Env.require(null));
        assertThatNullPointerException().isThrownBy(() -> Env.getInt(null));
        assertThatNullPointerException().isThrownBy(() -> Env.getInt(null, 0));
        assertThatNullPointerException().isThrownBy(() -> Env.getLong(null));
        assertThatNullPointerException().isThrownBy(() -> Env.getBoolean((String) null));
        assertThatNullPointerException().isThrownBy(() -> Env.getBoolean(null, false));
    }
}
