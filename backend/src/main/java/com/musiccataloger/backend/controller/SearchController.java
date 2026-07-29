package com.musiccataloger.backend.controller;

import com.musiccataloger.backend.dto.search.AlbumDto;
import com.musiccataloger.backend.service.SearchService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * Proxy endpoint for the iTunes Search API.
 *
 * GET /api/search?query=coldplay&type=album&limit=10
 *
 * Security rationale for proxying:
 * - Prevents frontend from calling Apple directly (avoids CORS issues and exposes no API keys).
 * - Input is validated server-side before forwarding.
 * - Response is filtered to only the fields the frontend needs.
 *
 * This endpoint requires a valid JWT (governed by SecurityConfig — all non-auth routes are protected).
 */
@Validated
@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;

    @GetMapping
    public ResponseEntity<List<AlbumDto>> search(

            @RequestParam
            @NotBlank(message = "Query must not be blank")
            @Size(max = 200, message = "Query must not exceed 200 characters")
            String query,

            @RequestParam(defaultValue = "album")
            String type,

            @RequestParam(defaultValue = "10")
            @Min(value = 1, message = "Limit must be at least 1")
            @Max(value = 25, message = "Limit must not exceed 25")
            int limit) {

        List<AlbumDto> results = searchService.search(query, type, limit);
        return ResponseEntity.ok(results);
    }
}
