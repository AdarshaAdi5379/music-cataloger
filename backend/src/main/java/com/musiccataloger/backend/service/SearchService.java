package com.musiccataloger.backend.service;

import com.musiccataloger.backend.client.ItunesApiClient;
import com.musiccataloger.backend.dto.search.AlbumDto;
import com.musiccataloger.backend.dto.search.ItunesResultItem;
import com.musiccataloger.backend.dto.search.ItunesSearchResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Orchestrates iTunes Search API calls.
 *
 * Responsibilities:
 * - Validates entity type against the supported set.
 * - Enforces minimum/maximum limits.
 * - Caches results by (query, type, limit) to reduce Apple API load.
 * - Maps iTunes response fields to the public {@link AlbumDto}.
 * - Upgrades artwork URLs from 100×100 to 600×600 resolution.
 * - Returns an empty list gracefully when no results are found.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class SearchService {

    private static final Set<String> SUPPORTED_TYPES = Set.of("album");
    static final int MAX_LIMIT = 25;

    private final ItunesApiClient itunesApiClient;

    /**
     * Search the iTunes catalog for albums matching {@code query}.
     *
     * @param query raw search term (validated, not blank)
     * @param type  entity type — currently only "album" is supported
     * @param limit number of results (1–25)
     * @return list of matched albums; empty list when no results
     * @throws IllegalArgumentException for unsupported types
     */
    @Cacheable(value = "searches", key = "#query.toLowerCase() + ':' + #type + ':' + #limit")
    public List<AlbumDto> search(String query, String type, int limit) {
        validateType(type);
        int effectiveLimit = Math.min(limit, MAX_LIMIT);

        ItunesSearchResponse response = itunesApiClient.search(query, type, effectiveLimit);

        if (response.results() == null || response.results().isEmpty()) {
            log.debug("iTunes Search returned 0 results");
            return Collections.emptyList();
        }

        return response.results().stream()
                .filter(item -> item.collectionId() != null)   // skip malformed entries
                .map(this::toAlbumDto)
                .collect(Collectors.toList());
    }

    // ── private helpers ───────────────────────────────────────────────────────

    private void validateType(String type) {
        if (!SUPPORTED_TYPES.contains(type.toLowerCase())) {
            throw new IllegalArgumentException(
                    "Unsupported search type '" + type + "'. Supported values: " + SUPPORTED_TYPES);
        }
    }

    private AlbumDto toAlbumDto(ItunesResultItem item) {
        // Upgrade 100×100 thumbnail to 600×600 for better display quality
        String artworkUrl = StringUtils.hasText(item.artworkUrl100())
                ? item.artworkUrl100().replace("100x100bb", "600x600bb")
                : null;

        return new AlbumDto(
                String.valueOf(item.collectionId()),
                item.collectionName(),
                item.artistName(),
                item.primaryGenreName(),
                item.releaseDate(),
                item.trackCount(),
                artworkUrl
        );
    }
}
