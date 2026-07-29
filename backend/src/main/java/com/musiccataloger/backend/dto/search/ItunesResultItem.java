package com.musiccataloger.backend.dto.search;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

/**
 * Internal DTO — maps a single item from the raw iTunes Search API JSON response.
 * Only the fields needed for the public AlbumDto are captured; all others are ignored.
 *
 * This type is never returned to callers; it is converted to {@link AlbumDto} in the service layer.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public record ItunesResultItem(

        @JsonProperty("collectionId")
        Long collectionId,

        @JsonProperty("collectionName")
        String collectionName,

        @JsonProperty("artistName")
        String artistName,

        @JsonProperty("primaryGenreName")
        String primaryGenreName,

        @JsonProperty("releaseDate")
        String releaseDate,

        @JsonProperty("trackCount")
        Integer trackCount,

        // iTunes returns e.g. "https://…/100x100bb.jpg"
        @JsonProperty("artworkUrl100")
        String artworkUrl100
) {}
