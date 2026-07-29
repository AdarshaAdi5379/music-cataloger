package com.musiccataloger.backend.client;

import com.musiccataloger.backend.dto.search.ItunesSearchResponse;
import com.musiccataloger.backend.exception.ItunesApiException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;

import java.time.Duration;
import java.util.concurrent.TimeoutException;

/**
 * Low-level HTTP client for the iTunes Search API.
 *
 * Timeout is injected via constructor so it can be overridden in unit tests
 * without needing a Spring context.
 */
@Slf4j
@Component
public class ItunesApiClient {

    private final WebClient itunesWebClient;
    private final long timeoutMs;

    /**
     * Production constructor — Spring injects the WebClient bean and reads
     * {@code itunes.api.timeout-ms} from configuration (default 5000 ms).
     */
    public ItunesApiClient(
            WebClient itunesWebClient,
            @Value("${itunes.api.timeout-ms:5000}") long timeoutMs) {
        this.itunesWebClient = itunesWebClient;
        this.timeoutMs = timeoutMs;
    }

    /**
     * Searches the iTunes catalog and returns the raw response envelope.
     *
     * @param term   search term (validated by caller)
     * @param entity iTunes entity type e.g. "album"
     * @param limit  max results 1–25
     * @return parsed response; never null
     * @throws ItunesApiException on any transport or API error
     */
    public ItunesSearchResponse search(String term, String entity, int limit) {
        log.debug("Calling iTunes Search API [entity={}, limit={}]", entity, limit);

        try {
            ItunesSearchResponse response = itunesWebClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/search")
                            .queryParam("term", term)
                            .queryParam("entity", entity)
                            .queryParam("limit", limit)
                            .queryParam("media", "music")
                            .build())
                    .retrieve()
                    .bodyToMono(ItunesSearchResponse.class)
                    .timeout(Duration.ofMillis(timeoutMs))
                    .onErrorMap(TimeoutException.class,
                            e -> new ItunesApiException("iTunes API request timed out", e))
                    .onErrorMap(WebClientResponseException.class,
                            e -> new ItunesApiException(
                                    "iTunes API returned HTTP " + e.getStatusCode().value(), e))
                    .onErrorMap(e -> !(e instanceof ItunesApiException),
                            e -> new ItunesApiException("iTunes API request failed", e))
                    .block();

            if (response == null) {
                throw new ItunesApiException("iTunes API returned an empty body");
            }
            return response;

        } catch (ItunesApiException e) {
            throw e;
        } catch (Exception e) {
            throw new ItunesApiException("Unexpected error calling iTunes API", e);
        }
    }
}
