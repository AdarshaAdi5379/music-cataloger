package com.musiccataloger.backend.dto.search;

/**
 * Public response DTO — the only album fields exposed to frontend callers.
 * Immutable record; password hashes and internal IDs are never included.
 */
public record AlbumDto(
        String appleCatalogId,
        String title,
        String artistName,
        String genre,
        String releaseDate,
        Integer trackCount,
        String artworkUrl
) {}
