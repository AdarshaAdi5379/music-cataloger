package com.musiccataloger.backend.search;

import com.musiccataloger.backend.client.ItunesApiClient;
import com.musiccataloger.backend.dto.search.ItunesSearchResponse;
import com.musiccataloger.backend.exception.ItunesApiException;
import okhttp3.mockwebserver.MockResponse;
import okhttp3.mockwebserver.MockWebServer;
import okhttp3.mockwebserver.SocketPolicy;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;

import java.io.IOException;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

/**
 * Unit tests for {@link ItunesApiClient} using {@link MockWebServer}.
 * No Spring context — pure HTTP-level testing.
 * Timeout is set to 2 s in all tests; the delay tests use 4 s to reliably trigger it.
 */
class ItunesApiClientTest {

    private static final long TIMEOUT_MS = 2_000L;

    private MockWebServer mockWebServer;
    private ItunesApiClient client;

    @BeforeEach
    void setUp() throws IOException {
        mockWebServer = new MockWebServer();
        mockWebServer.start();

        WebClient webClient = WebClient.builder()
                .baseUrl(mockWebServer.url("/").toString())
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        client = new ItunesApiClient(webClient, TIMEOUT_MS);
    }

    @AfterEach
    void tearDown() throws IOException {
        mockWebServer.shutdown();
    }

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("search() returns parsed response on 200 OK")
    void search_success_returnsResponse() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("""
                        {
                          "resultCount": 1,
                          "results": [{
                            "collectionId": 203562704,
                            "collectionName": "Parachutes",
                            "artistName": "Coldplay",
                            "primaryGenreName": "Alternative",
                            "releaseDate": "2000-07-10T07:00:00Z",
                            "trackCount": 10,
                            "artworkUrl100": "https://example.com/100x100bb.jpg"
                          }]
                        }
                        """));

        ItunesSearchResponse response = client.search("coldplay", "album", 10);

        assertThat(response).isNotNull();
        assertThat(response.resultCount()).isEqualTo(1);
        assertThat(response.results()).hasSize(1);
        assertThat(response.results().get(0).collectionName()).isEqualTo("Parachutes");
        assertThat(response.results().get(0).artistName()).isEqualTo("Coldplay");
    }

    // ── Empty results ─────────────────────────────────────────────────────────

    @Test
    @DisplayName("search() with no results returns response with empty list")
    void search_emptyResults_returnsEmptyList() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"resultCount\":0,\"results\":[]}"));

        ItunesSearchResponse response = client.search("xyzzy_no_match", "album", 5);

        assertThat(response.resultCount()).isEqualTo(0);
        assertThat(response.results()).isEmpty();
    }

    // ── Network failure ───────────────────────────────────────────────────────

    @Test
    @DisplayName("search() throws ItunesApiException when server closes connection")
    void search_networkFailure_throwsItunesApiException() {
        mockWebServer.enqueue(new MockResponse()
                .setSocketPolicy(SocketPolicy.DISCONNECT_AT_START));

        assertThatThrownBy(() -> client.search("coldplay", "album", 10))
                .isInstanceOf(ItunesApiException.class);
    }

    // ── HTTP 500 ──────────────────────────────────────────────────────────────

    @Test
    @DisplayName("search() throws ItunesApiException on HTTP 500")
    void search_serverError_throwsItunesApiException() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(500)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("{\"error\":\"server error\"}"));

        assertThatThrownBy(() -> client.search("coldplay", "album", 10))
                .isInstanceOf(ItunesApiException.class)
                .hasMessageContaining("500");
    }

    // ── Malformed JSON ────────────────────────────────────────────────────────

    @Test
    @DisplayName("search() throws ItunesApiException on malformed JSON")
    void search_malformedJson_throwsItunesApiException() {
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBody("NOT_VALID_JSON{{{{"));

        assertThatThrownBy(() -> client.search("coldplay", "album", 10))
                .isInstanceOf(ItunesApiException.class);
    }

    // ── Timeout ───────────────────────────────────────────────────────────────

    @Test
    @DisplayName("search() throws ItunesApiException when response exceeds timeout")
    void search_timeout_throwsItunesApiException() {
        // Delay the body by 4 s; client timeout is 2 s
        mockWebServer.enqueue(new MockResponse()
                .setResponseCode(200)
                .setHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON_VALUE)
                .setBodyDelay(4, TimeUnit.SECONDS)
                .setBody("{\"resultCount\":0,\"results\":[]}"));

        assertThatThrownBy(() -> client.search("coldplay", "album", 10))
                .isInstanceOf(ItunesApiException.class)
                .hasMessageContaining("timed out");
    }
}
