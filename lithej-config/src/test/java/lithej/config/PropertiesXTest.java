package lithej.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.util.NoSuchElementException;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Properties;
import lithej.io.FileIO;
import lithej.io.ResourceNotFoundException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class PropertiesXTest {

    @TempDir
    Path tempDir;

    @Test
    void loadReadsUtf8PropertiesFile() {
        Path file = tempDir.resolve("app.properties");
        FileIO.write(file, "name=café\nport=8080\nenabled=true\n");

        Properties properties = PropertiesX.load(file);

        assertThat(properties.getProperty("name")).isEqualTo("café");
        assertThat(properties.getProperty("port")).isEqualTo("8080");
    }

    @Test
    void loadWithExplicitCharset() throws Exception {
        Path file = tempDir.resolve("latin1.properties");
        java.nio.file.Files.write(file, "name=hello\n".getBytes(StandardCharsets.ISO_8859_1));

        Properties properties = PropertiesX.load(file, StandardCharsets.ISO_8859_1);

        assertThat(properties.getProperty("name")).isEqualTo("hello");
    }

    @Test
    void loadFromResourceReadsClasspathFile() {
        Properties properties = PropertiesX.loadFromResource("testdata/app.properties");
        assertThat(properties.getProperty("greeting")).isEqualTo("hello from resource");
    }

    @Test
    void loadFromResourceThrowsForMissingResource() {
        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> PropertiesX.loadFromResource("testdata/missing.properties"));
    }

    @Test
    void requireReturnsValueWhenSet() {
        Properties properties = new Properties();
        properties.setProperty("key", "value");
        assertThat(PropertiesX.require(properties, "key")).isEqualTo("value");
    }

    @Test
    void requireThrowsWhenMissing() {
        Properties properties = new Properties();
        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(() -> PropertiesX.require(properties, "missing"))
                .withMessageContaining("missing");
    }

    @Test
    void getStringReturnsEmptyWhenMissing() {
        Properties properties = new Properties();
        assertThat(PropertiesX.getString(properties, "missing")).isEqualTo(Optional.empty());
    }

    @Test
    void getStringWithDefaultUsesDefaultWhenMissing() {
        Properties properties = new Properties();
        assertThat(PropertiesX.getString(properties, "missing", "fallback")).isEqualTo("fallback");
    }

    @Test
    void getIntParsesValidValues() {
        Properties properties = new Properties();
        properties.setProperty("port", "8080");
        assertThat(PropertiesX.getInt(properties, "port")).isEqualTo(OptionalInt.of(8080));
        assertThat(PropertiesX.getInt(properties, "port", 0)).isEqualTo(8080);
    }

    @Test
    void getIntReturnsDefaultForMissingOrInvalidValues() {
        Properties properties = new Properties();
        properties.setProperty("port", "not-a-number");
        assertThat(PropertiesX.getInt(properties, "port")).isEqualTo(OptionalInt.empty());
        assertThat(PropertiesX.getInt(properties, "port", 42)).isEqualTo(42);
        assertThat(PropertiesX.getInt(properties, "missing", 42)).isEqualTo(42);
    }

    @Test
    void getBooleanParsesOrFallsBackToDefault() {
        Properties properties = new Properties();
        properties.setProperty("enabled", "true");
        assertThat(PropertiesX.getBoolean(properties, "enabled", false)).isTrue();
        assertThat(PropertiesX.getBoolean(properties, "missing", true)).isTrue();
        assertThat(PropertiesX.getBoolean(properties, "missing", false)).isFalse();
    }

    @Test
    void allAccessorsRejectNullArguments() {
        Properties properties = new Properties();
        assertThatNullPointerException().isThrownBy(() -> PropertiesX.load(null));
        assertThatNullPointerException().isThrownBy(() -> PropertiesX.load(tempDir, null));
        assertThatNullPointerException().isThrownBy(() -> PropertiesX.loadFromResource(null));
        assertThatNullPointerException().isThrownBy(() -> PropertiesX.require(null, "k"));
        assertThatNullPointerException().isThrownBy(() -> PropertiesX.require(properties, null));
        assertThatNullPointerException().isThrownBy(() -> PropertiesX.getString(null, "k"));
        assertThatNullPointerException().isThrownBy(() -> PropertiesX.getString(properties, null));
        assertThatNullPointerException().isThrownBy(() -> PropertiesX.getInt(null, "k"));
        assertThatNullPointerException().isThrownBy(() -> PropertiesX.getBoolean(null, "k", false));
    }
}
