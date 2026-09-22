package lithej.net;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import org.junit.jupiter.api.Test;

class HttpOptionsTest {

    @Test
    void defaultsHaveNoHeadersQueryParamsTimeoutOrClient() {
        HttpOptions options = HttpOptions.defaults();
        assertThat(options.headers()).isEmpty();
        assertThat(options.queryParams()).isEmpty();
        assertThat(options.timeout()).isNull();
        assertThat(options.client()).isNull();
    }

    @Test
    void withHeaderAccumulatesAndLastValueForANameWins() {
        HttpOptions options = HttpOptions.defaults()
                .withHeader("A", "1")
                .withHeader("B", "2")
                .withHeader("A", "override");

        assertThat(options.headers()).containsEntry("A", "override").containsEntry("B", "2");
    }

    @Test
    void withHeaderRejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> HttpOptions.defaults().withHeader(null, "v"));
        assertThatNullPointerException().isThrownBy(() -> HttpOptions.defaults().withHeader("n", null));
    }

    @Test
    void withHeadersMergesAndOverridesExisting() {
        HttpOptions options = HttpOptions.defaults()
                .withHeader("A", "1")
                .withHeaders(Map.of("A", "2", "B", "3"));

        assertThat(options.headers()).containsEntry("A", "2").containsEntry("B", "3");
    }

    @Test
    void withHeadersRejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> HttpOptions.defaults().withHeaders(null));
    }

    @Test
    void withQueryParamAccumulatesAndLastValueForANameWins() {
        HttpOptions options = HttpOptions.defaults()
                .withQueryParam("q", "first")
                .withQueryParam("q", "second")
                .withQueryParam("page", "2");

        assertThat(options.queryParams()).containsEntry("q", "second").containsEntry("page", "2");
    }

    @Test
    void withQueryParamRejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> HttpOptions.defaults().withQueryParam(null, "v"));
        assertThatNullPointerException().isThrownBy(() -> HttpOptions.defaults().withQueryParam("n", null));
    }

    @Test
    void withTimeoutReturnsNewInstance() {
        HttpOptions options = HttpOptions.defaults().withTimeout(Duration.ofSeconds(5));
        assertThat(options.timeout()).isEqualTo(Duration.ofSeconds(5));
    }

    @Test
    void withTimeoutRejectsNullZeroAndNegative() {
        assertThatNullPointerException().isThrownBy(() -> HttpOptions.defaults().withTimeout(null));
        assertThatIllegalArgumentException().isThrownBy(() -> HttpOptions.defaults().withTimeout(Duration.ZERO));
        assertThatIllegalArgumentException()
                .isThrownBy(() -> HttpOptions.defaults().withTimeout(Duration.ofSeconds(-1)));
    }

    @Test
    void withClientReturnsNewInstanceHoldingTheSuppliedClient() {
        HttpClient client = HttpClient.newHttpClient();
        HttpOptions options = HttpOptions.defaults().withClient(client);
        assertThat(options.client()).isSameAs(client);
    }

    @Test
    void withClientRejectsNull() {
        assertThatNullPointerException().isThrownBy(() -> HttpOptions.defaults().withClient(null));
    }

    @Test
    void withXMethodsDoNotMutateTheReceiver() {
        HttpOptions original = HttpOptions.defaults();

        original.withHeader("A", "1");
        original.withQueryParam("q", "1");
        original.withTimeout(Duration.ofSeconds(1));

        assertThat(original.headers()).isEmpty();
        assertThat(original.queryParams()).isEmpty();
        assertThat(original.timeout()).isNull();
    }

    @Test
    void headersReturnsUnmodifiableCopy() {
        HttpOptions options = HttpOptions.defaults().withHeader("A", "1");
        Map<String, String> headers = options.headers();
        assertThatExceptionOfType(UnsupportedOperationException.class).isThrownBy(() -> headers.put("B", "2"));
    }

    @Test
    void queryParamsReturnsUnmodifiableCopy() {
        HttpOptions options = HttpOptions.defaults().withQueryParam("q", "1");
        Map<String, String> queryParams = options.queryParams();
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> queryParams.put("r", "2"));
    }

    @Test
    void defaultInstancesAreEqual() {
        assertThat(HttpOptions.defaults()).isEqualTo(HttpOptions.defaults()).hasSameHashCodeAs(HttpOptions.defaults());
    }

    @Test
    void addingAHeaderMakesInstancesUnequal() {
        assertThat(HttpOptions.defaults()).isNotEqualTo(HttpOptions.defaults().withHeader("A", "1"));
    }

    @Test
    void equalsAndHashCodeReflectAllFields() {
        HttpOptions a = HttpOptions.defaults().withHeader("A", "1");
        HttpOptions b = HttpOptions.defaults().withHeader("A", "1");
        HttpOptions c = HttpOptions.defaults().withHeader("A", "2");

        assertThat(a).isEqualTo(b).hasSameHashCodeAs(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(a).isNotEqualTo("not an options object");
        assertThat(a).isEqualTo(a);
    }

    @Test
    void toStringRedactsSensitiveHeaderValues() {
        HttpOptions options = HttpOptions.defaults()
                .withHeader("Authorization", "Bearer super-secret-token")
                .withHeader("X-Api-Key", "another-secret-value")
                .withHeader("Accept", "application/json");

        String text = options.toString();

        assertThat(text).doesNotContain("super-secret-token").doesNotContain("another-secret-value");
        assertThat(text).contains("application/json");
    }

    @Test
    void toStringDoesNotThrowAndMentionsClassName() {
        assertThat(HttpOptions.defaults().toString()).contains("HttpOptions");
    }
}
