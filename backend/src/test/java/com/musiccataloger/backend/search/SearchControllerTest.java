package com.musiccataloger.backend.search;

import com.musiccataloger.backend.dto.search.AlbumDto;
import com.musiccataloger.backend.exception.ItunesApiException;
import com.musiccataloger.backend.service.SearchService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Integration tests for the search endpoint.
 * SearchService is mocked so no real HTTP calls are made to Apple.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SearchControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private SearchService searchService;

    private static final String SEARCH_URL = "/api/search";

    private static final AlbumDto PARACHUTES = new AlbumDto(
            "203562704", "Parachutes", "Coldplay", "Alternative",
            "2000-07-10T07:00:00Z", 10,
            "https://is1-ssl.mzstatic.com/image/thumb/test/600x600bb.jpg");

    // ── Valid search ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /api/search valid query returns 200 with album list")
    void search_validQuery_returns200() throws Exception {
        when(searchService.search("coldplay", "album", 10))
                .thenReturn(List.of(PARACHUTES));

        mockMvc.perform(get(SEARCH_URL)
                        .param("query", "coldplay")
                        .param("type", "album")
                        .param("limit", "10"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].appleCatalogId").value("203562704"))
                .andExpect(jsonPath("$[0].title").value("Parachutes"))
                .andExpect(jsonPath("$[0].artistName").value("Coldplay"))
                .andExpect(jsonPath("$[0].genre").value("Alternative"))
                .andExpect(jsonPath("$[0].trackCount").value(10))
                // password or internal fields must never appear
                .andExpect(jsonPath("$[0].password").doesNotExist());
    }

    // ── Empty search ──────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /api/search with no results returns 200 with empty array")
    void search_noResults_returns200EmptyList() throws Exception {
        when(searchService.search(anyString(), anyString(), anyInt()))
                .thenReturn(List.of());

        mockMvc.perform(get(SEARCH_URL)
                        .param("query", "xyzzy_nonexistent_album_12345"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(0));
    }

    // ── Invalid entity type ───────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /api/search with unsupported type returns 400")
    void search_invalidType_returns400() throws Exception {
        when(searchService.search(anyString(), anyString(), anyInt()))
                .thenThrow(new IllegalArgumentException("Unsupported search type 'podcast'"));

        mockMvc.perform(get(SEARCH_URL)
                        .param("query", "coldplay")
                        .param("type", "podcast"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ── Special characters ────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /api/search with special characters in query returns 200")
    void search_specialCharacters_returns200() throws Exception {
        when(searchService.search("AC/DC & friends <rock>", "album", 10))
                .thenReturn(List.of());

        mockMvc.perform(get(SEARCH_URL)
                        .param("query", "AC/DC & friends <rock>"))
                .andExpect(status().isOk());
    }

    // ── Blank query ───────────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /api/search with blank query returns 400")
    void search_blankQuery_returns400() throws Exception {
        mockMvc.perform(get(SEARCH_URL)
                        .param("query", "   "))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }

    // ── Limit validation ──────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /api/search with limit > 25 returns 400")
    void search_limitExceedsMax_returns400() throws Exception {
        mockMvc.perform(get(SEARCH_URL)
                        .param("query", "coldplay")
                        .param("limit", "100"))
                .andExpect(status().isBadRequest());
    }

    // ── Network failure ───────────────────────────────────────────────────────

    @Test
    @WithMockUser
    @DisplayName("GET /api/search when iTunes API fails returns 502")
    void search_networkFailure_returns502() throws Exception {
        when(searchService.search(anyString(), anyString(), anyInt()))
                .thenThrow(new ItunesApiException("Connection refused"));

        mockMvc.perform(get(SEARCH_URL)
                        .param("query", "coldplay"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").value(502))
                // Internal error message must NOT be exposed
                .andExpect(jsonPath("$.message")
                        .value("Music catalog service temporarily unavailable"));
    }

    // ── Unauthenticated ───────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/search without token returns 403")
    void search_unauthenticated_returns403() throws Exception {
        mockMvc.perform(get(SEARCH_URL)
                        .param("query", "coldplay"))
                .andExpect(status().isForbidden());
    }
}
