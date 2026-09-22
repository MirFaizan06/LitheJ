package lithej.net;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatIllegalArgumentException;
import static org.assertj.core.api.Assertions.assertThatNullPointerException;

import java.io.IOException;
import java.net.http.HttpClient;
import java.time.Duration;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.RecordedRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * All requests in this suite are made against a local {@link MockWebServer}, never
 * real network hosts, so the suite is fast, deterministic, and runs offline.
 */
class HttpTest {

    private MockWebServer server;

    @BeforeEach
    void startServer() throws IOException {
        server = new MockWebServer();
        server.start();
    }

    @AfterEach
    void stopServer() throws IOException {
        server.shutdown();
    }

    @Test
    void getReturnsStatusBodyAndHeaders() throws InterruptedException {
        server.enqueue(new MockResponse()
                .setResponseCode(200)
                .setBody("hello")
                .addHeader("X-Test", "value1"));

        HttpResponse response = Http.get(server.url("/hello").toString());

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("hello");
        assertThat(response.isSuccessful()).isTrue();
        assertThat(response.headers().get("X-Test")).containsExactly("value1");

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("GET");
        assertThat(recorded.getPath()).isEqualTo("/hello");
    }

    @Test
    void postSendsBodyAndReturnsResponse() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(201).setBody("created"));

        HttpResponse response = Http.post(server.url("/items").toString(), "payload");

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).isEqualTo("created");

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("POST");
        assertThat(recorded.getPath()).isEqualTo("/items");
        assertThat(recorded.getBody().readUtf8()).isEqualTo("payload");
    }

    @Test
    void putSendsBodyAndReturnsResponse() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("updated"));

        HttpResponse response = Http.put(server.url("/items/1").toString(), "new-value");

        assertThat(response.statusCode()).isEqualTo(200);
        assertThat(response.body()).isEqualTo("updated");

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("PUT");
        assertThat(recorded.getPath()).isEqualTo("/items/1");
        assertThat(recorded.getBody().readUtf8()).isEqualTo("new-value");
    }

    @Test
    void deleteReturnsResponse() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(204));

        HttpResponse response = Http.delete(server.url("/items/1").toString());

        assertThat(response.statusCode()).isEqualTo(204);

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getMethod()).isEqualTo("DELETE");
        assertThat(recorded.getPath()).isEqualTo("/items/1");
    }

    @Test
    void customHeadersArriveAtTheServer() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200));

        HttpOptions options = HttpOptions.defaults()
                .withHeader("X-Custom", "abc")
                .withHeaders(Map.of("X-Other", "def"));
        Http.get(server.url("/").toString(), options);

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getHeader("X-Custom")).isEqualTo("abc");
        assertThat(recorded.getHeader("X-Other")).isEqualTo("def");
    }

    @Test
    void repeatedHeaderNameLastValueWinsOnTheWire() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200));

        HttpOptions options = HttpOptions.defaults()
                .withHeader("X-Val", "first")
                .withHeader("X-Val", "second");
        assertThat(options.headers()).containsEntry("X-Val", "second");

        Http.get(server.url("/").toString(), options);

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getHeader("X-Val")).isEqualTo("second");
    }

    @Test
    void queryParamsAreEncodedAndAppendedToTheUrl() throws InterruptedException {
        server.enqueue(new MockResponse().setResponseCode(200));

        HttpOptions options = HttpOptions.defaults().withQueryParam("q", "a b&c");
        Http.get(server.url("/search").toString(), options);

        RecordedRequest recorded = server.takeRequest();
        assertThat(recorded.getPath()).isEqualTo("/search?q=a+b%26c");
    }

    @Test
    void non2xxStatusesDoNotThrow() {
        server.enqueue(new MockResponse().setResponseCode(404).setBody("missing"));
        HttpResponse notFound = Http.get(server.url("/missing").toString());
        assertThat(notFound.statusCode()).isEqualTo(404);
        assertThat(notFound.isSuccessful()).isFalse();
        assertThat(notFound.body()).isEqualTo("missing");

        server.enqueue(new MockResponse().setResponseCode(500).setBody("boom"));
        HttpResponse serverError = Http.get(server.url("/boom").toString());
        assertThat(serverError.statusCode()).isEqualTo(500);
        assertThat(serverError.isSuccessful()).isFalse();
    }

    @Test
    void malformedUrlThrowsIllegalArgumentException() {
        assertThatIllegalArgumentException()
                .isThrownBy(() -> Http.get("not a url with spaces and no scheme"));
    }

    @Test
    void requestTimeoutThrowsHttpRequestException() {
        server.enqueue(new MockResponse().setBody("late").setBodyDelay(5, TimeUnit.SECONDS));

        HttpOptions options = HttpOptions.defaults().withTimeout(Duration.ofMillis(300));

        assertThatExceptionOfType(HttpRequestException.class)
                .isThrownBy(() -> Http.get(server.url("/slow").toString(), options));
    }

    @Test
    void connectionFailureThrowsHttpRequestException() throws IOException {
        MockWebServer deadServer = new MockWebServer();
        deadServer.start();
        String url = deadServer.url("/").toString();
        deadServer.shutdown();

        assertThatExceptionOfType(HttpRequestException.class).isThrownBy(() -> Http.get(url));
    }

    @Test
    void withClientUsesTheSuppliedClient() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("ok"));

        HttpClient customClient = HttpClient.newHttpClient();
        HttpOptions options = HttpOptions.defaults().withClient(customClient);

        HttpResponse response = Http.get(server.url("/").toString(), options);

        assertThat(response.body()).isEqualTo("ok");
    }

    @Test
    void getRejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> Http.get(null));
        assertThatNullPointerException().isThrownBy(() -> Http.get(null, HttpOptions.defaults()));
        assertThatNullPointerException()
                .isThrownBy(() -> Http.get(server.url("/").toString(), null));
    }

    @Test
    void postRejectsNullArguments() {
        String url = server.url("/").toString();
        assertThatNullPointerException().isThrownBy(() -> Http.post(null, "body"));
        assertThatNullPointerException().isThrownBy(() -> Http.post(url, null));
        assertThatNullPointerException().isThrownBy(() -> Http.post(url, "body", null));
    }

    @Test
    void putRejectsNullArguments() {
        String url = server.url("/").toString();
        assertThatNullPointerException().isThrownBy(() -> Http.put(null, "body"));
        assertThatNullPointerException().isThrownBy(() -> Http.put(url, null));
        assertThatNullPointerException().isThrownBy(() -> Http.put(url, "body", null));
    }

    @Test
    void deleteRejectsNullArguments() {
        assertThatNullPointerException().isThrownBy(() -> Http.delete(null));
        assertThatNullPointerException().isThrownBy(() -> Http.delete(null, HttpOptions.defaults()));
        assertThatNullPointerException()
                .isThrownBy(() -> Http.delete(server.url("/").toString(), null));
    }
}
