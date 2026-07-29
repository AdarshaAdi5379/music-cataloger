package com.musiccataloger.backend.config;

import io.netty.channel.ChannelOption;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;

/**
 * Configures the {@link WebClient} bean used to call the iTunes Search API.
 *
 * Timeouts are enforced at both the connection and response levels to protect
 * against hanging external calls.  The base URL is loaded from configuration
 * so it can be overridden in tests or other environments.
 */
@Configuration
public class WebClientConfig {

    @Value("${itunes.api.base-url}")
    private String baseUrl;

    @Value("${itunes.api.timeout-ms:5000}")
    private int timeoutMs;

    @Bean
    public WebClient itunesWebClient(WebClient.Builder builder) {
        HttpClient httpClient = HttpClient.create()
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, timeoutMs)
                .responseTimeout(Duration.ofMillis(timeoutMs));

        return builder
                .baseUrl(baseUrl)
                .clientConnector(new ReactorClientHttpConnector(httpClient))
                .defaultHeader(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                // Guard against unexpectedly large Apple responses
                .codecs(cfg -> cfg.defaultCodecs().maxInMemorySize(512 * 1024))
                .build();
    }
}
