package lithej.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Properties;
import org.junit.jupiter.api.Test;

class ConfigTest {

    @Test
    void emptyConfigReturnsEmptyForAnyKey() {
        assertThat(Config.empty().get("anything")).isEmpty();
    }

    @Test
    void withDefaultProvidesFallbackValue() {
        Config config = Config.empty().withDefault("port", "8080");
        assertThat(config.get("port")).contains("8080");
        assertThat(config.get("other")).isEmpty();
    }

    @Test
    void earlierLayerTakesPrecedenceOverLater() {
        Config config = Config.empty()
                .withMap(Map.of("port", "9090"))
                .withDefault("port", "8080");

        assertThat(config.get("port")).contains("9090");
    }

    @Test
    void laterLayerIsUsedWhenEarlierLayerLacksTheKey() {
        Config config = Config.empty()
                .withMap(Map.of("host", "localhost"))
                .withDefault("port", "8080");

        assertThat(config.get("port")).contains("8080");
        assertThat(config.get("host")).contains("localhost");
    }

    @Test
    void withPropertiesAddsALayer() {
        Properties properties = new Properties();
        properties.setProperty("name", "lithej");
        Config config = Config.empty().withProperties(properties);
        assertThat(config.get("name")).contains("lithej");
    }

    @Test
    void withSystemPropertiesReadsLiveSystemProperties() {
        System.setProperty("lithej.config.test.key", "system-value");
        try {
            Config config = Config.empty().withSystemProperties();
            assertThat(config.get("lithej.config.test.key")).contains("system-value");
        } finally {
            System.clearProperty("lithej.config.test.key");
        }
    }

    @Test
    void withEnvironmentVariablesAddsALayer() {
        Config config = Config.empty().withEnvironmentVariables();
        assertThat(config.get("LITHEJ_CONFIG_TEST_DEFINITELY_UNSET_VAR")).isEmpty();
    }

    @Test
    void requireReturnsValueWhenPresent() {
        Config config = Config.empty().withDefault("key", "value");
        assertThat(config.require("key")).isEqualTo("value");
    }

    @Test
    void requireThrowsWhenAbsent() {
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> Config.empty().require("missing"))
                .withMessageContaining("missing");
    }

    @Test
    void getOrDefaultFallsBackWhenAbsent() {
        assertThat(Config.empty().getOrDefault("missing", "fallback")).isEqualTo("fallback");
    }

    @Test
    void getIntParsesOrFallsBackToDefault() {
        Config config = Config.empty().withDefault("port", "8080");
        assertThat(config.getInt("port", 0)).isEqualTo(8080);
        assertThat(config.getInt("missing", 42)).isEqualTo(42);

        Config invalid = Config.empty().withDefault("port", "not-a-number");
        assertThat(invalid.getInt("port", 7)).isEqualTo(7);
    }

    @Test
    void getBooleanParsesOrFallsBackToDefault() {
        Config config = Config.empty().withDefault("enabled", "true");
        assertThat(config.getBoolean("enabled", false)).isTrue();
        assertThat(config.getBoolean("missing", true)).isTrue();
    }

    @Test
    void withMethodsReturnNewInstancesWithoutMutatingOriginal() {
        Config original = Config.empty();
        Config withDefault = original.withDefault("k", "v");

        assertThat(original.get("k")).isEmpty();
        assertThat(withDefault.get("k")).contains("v");
    }

    @Test
    void withMethodsRejectNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> Config.empty().withProperties(null));
        assertThatNullPointerException().isThrownBy(() -> Config.empty().withMap(null));
        assertThatNullPointerException().isThrownBy(() -> Config.empty().withDefault(null, "v"));
        assertThatNullPointerException().isThrownBy(() -> Config.empty().withDefault("k", null));
    }

    @Test
    void accessorsRejectNullKey() {
        Config config = Config.empty();
        assertThatNullPointerException().isThrownBy(() -> config.get(null));
        assertThatNullPointerException().isThrownBy(() -> config.require(null));
        assertThatNullPointerException().isThrownBy(() -> config.getOrDefault(null, "x"));
        assertThatNullPointerException().isThrownBy(() -> config.getInt(null, 0));
        assertThatNullPointerException().isThrownBy(() -> config.getBoolean(null, false));
    }
}
