package lithej.io;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.io.InputStream;
import java.util.List;
import org.junit.jupiter.api.Test;

class ResourcesTest {

    private static final String EXISTING = "testdata/sample.txt";
    private static final String MISSING = "testdata/does-not-exist.txt";

    @Test
    void readReturnsUtf8DecodedContent() {
        String content = Resources.read(EXISTING);
        assertThat(content).contains("line one").contains("line two").contains("café");
    }

    @Test
    void readLinesSplitsContentIntoLines() {
        List<String> lines = Resources.readLines(EXISTING);
        assertThat(lines).containsExactly("line one", "line two", "café");
    }

    @Test
    void readBytesReturnsRawContent() {
        byte[] bytes = Resources.readBytes(EXISTING);
        assertThat(bytes.length).isGreaterThan(0);
        assertThat(new String(bytes, java.nio.charset.StandardCharsets.UTF_8)).contains("line one");
    }

    @Test
    void existsReflectsResourcePresence() {
        assertThat(Resources.exists(EXISTING)).isTrue();
        assertThat(Resources.exists(MISSING)).isFalse();
    }

    @Test
    void openReturnsAnOpenStreamForExistingResource() throws Exception {
        try (InputStream in = Resources.open(EXISTING)) {
            assertThat(in.readAllBytes().length).isGreaterThan(0);
        }
    }

    @Test
    void openThrowsResourceNotFoundExceptionForMissingResource() {
        assertThatExceptionOfType(ResourceNotFoundException.class)
                .isThrownBy(() -> Resources.open(MISSING))
                .withMessageContaining(MISSING);
    }

    @Test
    void readThrowsResourceNotFoundExceptionForMissingResource() {
        assertThatExceptionOfType(ResourceNotFoundException.class).isThrownBy(() -> Resources.read(MISSING));
    }

    @Test
    void fallsBackToDefiningClassLoaderWhenNoContextClassLoaderIsSet() {
        Thread current = Thread.currentThread();
        ClassLoader original = current.getContextClassLoader();
        try {
            current.setContextClassLoader(null);
            assertThat(Resources.exists(EXISTING)).isTrue();
        } finally {
            current.setContextClassLoader(original);
        }
    }

    @Test
    void methodsRejectNullResourcePath() {
        assertThatNullPointerException().isThrownBy(() -> Resources.read(null));
        assertThatNullPointerException().isThrownBy(() -> Resources.readLines(null));
        assertThatNullPointerException().isThrownBy(() -> Resources.readBytes(null));
        assertThatNullPointerException().isThrownBy(() -> Resources.exists(null));
        assertThatNullPointerException().isThrownBy(() -> Resources.open(null));
    }
}
