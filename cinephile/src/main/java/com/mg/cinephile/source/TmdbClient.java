package com.mg.cinephile.source;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.util.Optional;

/**
 * Calls TMDB to enrich movies with synopsis, poster, release date,
 * country of origin, and IMDb ID.
 *
 * Flow:
 *   1. /search/movie?query=<cleaned title>  → first result's TMDB ID
 *   2. /movie/{tmdbId}                       → full details
 */
@Component
public class TmdbClient {

    private static final Logger log = LoggerFactory.getLogger(TmdbClient.class);

    private final String apiKey;
    private final String imageBaseUrl;
    private final WebClient webClient;

    public TmdbClient(@Value("${tmdb.api.key:}") String apiKey,
                      @Value("${tmdb.api.base-url}") String baseUrl,
                      @Value("${tmdb.api.image-base-url}") String imageBaseUrl) {
        this.apiKey = apiKey;
        this.imageBaseUrl = imageBaseUrl;
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Search TMDB for a movie by title and return enriched data,
     * or empty if no match or TMDB isn't configured.
     */
    public Optional<TmdbResult> enrich(String rawTitle) {
        if (!isConfigured()) return Optional.empty();
        if (rawTitle == null || rawTitle.isBlank()) return Optional.empty();

        String cleanedTitle = cleanTitle(rawTitle);

        try {
            // Step 1 — search by title
            Long tmdbId = searchForTmdbId(cleanedTitle);
            if (tmdbId == null) {
                log.debug("TMDB: no match for '{}' (cleaned: '{}')", rawTitle, cleanedTitle);
                return Optional.empty();
            }

            // Step 2 — fetch full details
            return fetchDetails(tmdbId);

        } catch (Exception e) {
            log.warn("TMDB enrichment failed for '{}': {}", rawTitle, e.getMessage());
            return Optional.empty();
        }
    }

    /**
     * Strip festival names, format suffixes, and other AMC-added noise
     * from a movie title before searching TMDB.
     * "Ponyo - Studio Ghibli Fest 2026" → "Ponyo"
     * "Mission: Impossible (IMAX)" → "Mission: Impossible"
     */
    private String cleanTitle(String raw) {
        String t = raw;
        // Drop anything after " - " (often festival/series labels)
        int dashIdx = t.indexOf(" - ");
        if (dashIdx > 0) t = t.substring(0, dashIdx);
        // Drop parenthetical suffixes like "(IMAX)" "(4K)" "(Director's Cut)"
        int parenIdx = t.indexOf(" (");
        if (parenIdx > 0) t = t.substring(0, parenIdx);
        return t.trim();
    }

    private Long searchForTmdbId(String cleanedTitle) {
        JsonNode response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/search/movie")
                        .queryParam("api_key", apiKey)
                        .queryParam("query", cleanedTitle)
                        .build())
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null) return null;
        JsonNode results = response.path("results");
        if (!results.isArray() || results.isEmpty()) return null;

        // Take the first result. TMDB sorts by relevance/popularity.
        return results.get(0).path("id").asLong();
    }

    private Optional<TmdbResult> fetchDetails(Long tmdbId) {
        JsonNode response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/movie/{id}")
                        .queryParam("api_key", apiKey)
                        .build(tmdbId))
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        if (response == null) return Optional.empty();

        TmdbResult result = new TmdbResult();
        result.tmdbId = String.valueOf(tmdbId);
        result.imdbId = nullIfBlank(response.path("imdb_id").asText(null));
        result.synopsis = nullIfBlank(response.path("overview").asText(null));

        // Release date — TMDB returns "yyyy-MM-dd" or empty string
        String releaseStr = response.path("release_date").asText("");
        if (!releaseStr.isBlank()) {
            try {
                result.releaseDate = LocalDate.parse(releaseStr);
            } catch (Exception ignored) { }
        }

        // Poster URL — TMDB gives a path; we prepend the base
        String posterPath = response.path("poster_path").asText("");
        if (!posterPath.isBlank()) {
            result.posterUrl = imageBaseUrl + posterPath;
        }

        // Country of origin — TMDB returns an array; take the first
        JsonNode countries = response.path("production_countries");
        if (countries.isArray() && !countries.isEmpty()) {
            result.countryOfOrigin = nullIfBlank(
                    countries.get(0).path("iso_3166_1").asText(null));
        }

        return Optional.of(result);
    }

    private String nullIfBlank(String s) {
        return (s == null || s.isBlank() || "null".equals(s)) ? null : s;
    }

    /**
     * Plain data holder for enriched fields. Public-static so the
     * SeedLoader can read it directly without another file.
     */
    public static class TmdbResult {
        public String tmdbId;
        public String imdbId;
        public String synopsis;
        public LocalDate releaseDate;
        public String posterUrl;
        public String countryOfOrigin;
    }
}