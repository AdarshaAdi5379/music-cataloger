package com.musiccataloger.backend.dto.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

/**
 * Internal DTO — top-level iTunes Search API response envelope.
 * Never returned directly to callers.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ItunesSearchResponse(
        int resultCount,
        List<ItunesResultItem> results
) {}
