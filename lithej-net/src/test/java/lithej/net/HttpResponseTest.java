package lithej.net;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Exercises {@link HttpResponse} through real (local) exchanges made by {@link Http}
 * against {@link MockWebServer}, since {@code HttpResponse}'s constructor is
 * package-private and meant to be built only from a real
 * {@link java.net.http.HttpResponse}.
 */
class HttpResponseTest {

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
    void statusCodeAndBodyReflectTheResponse() {
        server.enqueue(new MockResponse().setResponseCode(201).setBody("created"));

        HttpResponse response = Http.get(server.url("/").toString());

        assertThat(response.statusCode()).isEqualTo(201);
        assertThat(response.body()).isEqualTo("created");
    }

    @Test
    void bodyDecodesAsUtf8() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("héllo"));

        HttpResponse response = Http.get(server.url("/").toString());

        assertThat(response.body()).isEqualTo("héllo");
    }

    @Test
    void bodyBytesReturnsADefensiveCopy() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("hello"));
        HttpResponse response = Http.get(server.url("/").toString());

        byte[] first = response.bodyBytes();
        first[0] = (byte) 'X';

        assertThat(response.bodyBytes()).isEqualTo("hello".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void isSuccessfulReflectsTheStatusRange() {
        server.enqueue(new MockResponse().setResponseCode(200));
        server.enqueue(new MockResponse().setResponseCode(299));
        server.enqueue(new MockResponse().setResponseCode(404));
        server.enqueue(new MockResponse().setResponseCode(500));

        assertThat(Http.get(server.url("/a").toString()).isSuccessful()).isTrue();
        assertThat(Http.get(server.url("/b").toString()).isSuccessful()).isTrue();
        assertThat(Http.get(server.url("/c").toString()).isSuccessful()).isFalse();
        assertThat(Http.get(server.url("/d").toString()).isSuccessful()).isFalse();
    }

    @Test
    void headersAreExposedAndUnmodifiable() {
        server.enqueue(new MockResponse().setResponseCode(200).addHeader("X-Test", "value1"));
        HttpResponse response = Http.get(server.url("/").toString());

        assertThat(response.headers().get("X-Test")).containsExactly("value1");

        Map<String, List<String>> headers = response.headers();
        assertThatExceptionOfType(UnsupportedOperationException.class)
                .isThrownBy(() -> headers.put("X-New", List.of("v")));
    }

    @Test
    void rawExposesTheUnderlyingJdkResponse() {
        server.enqueue(new MockResponse().setResponseCode(200).setBody("hi"));
        HttpResponse response = Http.get(server.url("/").toString());

        assertThat(response.raw()).isNotNull();
        assertThat(response.raw().statusCode()).isEqualTo(200);
        assertThat(response.raw().body()).isEqualTo("hi".getBytes(StandardCharsets.UTF_8));
    }

    @Test
    void toStringRedactsSensitiveResponseHeaderValues() {
        server.enqueue(new MockResponse().setResponseCode(200).addHeader("Set-Cookie", "session=abc123secret"));
        HttpResponse response = Http.get(server.url("/").toString());

        assertThat(response.toString()).doesNotContain("abc123secret");
    }

    @Test
    void toStringDoesNotThrowAndMentionsClassName() {
        server.enqueue(new MockResponse().setResponseCode(200));
        HttpResponse response = Http.get(server.url("/").toString());

        assertThat(response.toString()).contains("HttpResponse");
    }
}
