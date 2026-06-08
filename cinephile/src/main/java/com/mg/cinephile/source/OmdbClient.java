package com.mg.cinephile.source;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.Optional;

/**
 * Calls OMDb to fetch IMDb and Rotten Tomatoes ratings for a film.
 *
 * OMDb takes an IMDb id (which TMDB gives us) and returns a JSON
 * payload that includes the IMDb numeric rating and, when available,
 * a Rotten Tomatoes percentage tucked inside a "Ratings" array.
 */
@Component
public class OmdbClient {

    private static final Logger log = LoggerFactory.getLogger(OmdbClient.class);

    private final String apiKey;
    private final WebClient webClient;

    public OmdbClient(@Value("${omdb.api.key:}") String apiKey,
                      @Value("${omdb.api.base-url}") String baseUrl) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder().baseUrl(baseUrl).build();
    }

    public boolean isConfigured() {
        return apiKey != null && !apiKey.isBlank();
    }

    /**
     * Look up ratings by IMDb id. Returns empty if not configured,
     * no IMDb id, no match, or any error.
     */
    public Optional<OmdbResult> fetchRatings(String imdbId) {
        if (!isConfigured()) return Optional.empty();
        if (imdbId == null || imdbId.isBlank()) return Optional.empty();

        try {
            JsonNode response = webClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/")
                            .queryParam("i", imdbId)
                            .queryParam("apikey", apiKey)
                            .build())
                    .retrieve()
                    .bodyToMono(JsonNode.class)
                    .block();

            if (response == null) return Optional.empty();

            // OMDb returns {"Response": "False", "Error": "..."} on failure
            String responseOk = response.path("Response").asText("False");
            if (!"True".equals(responseOk)) return Optional.empty();

            OmdbResult result = new OmdbResult();

            // imdbRating is a string like "8.6" or "N/A"
            String imdbRatingStr = response.path("imdbRating").asText("N/A");
            if (!"N/A".equals(imdbRatingStr)) {
                try {
                    result.imdbRating = Double.parseDouble(imdbRatingStr);
                } catch (NumberFormatException ignored) { }
            }

            // Rotten Tomatoes lives inside a "Ratings" array like:
            //   "Ratings": [
            //     {"Source": "Internet Movie Database", "Value": "8.6/10"},
            //     {"Source": "Rotten Tomatoes", "Value": "97%"},
            //     {"Source": "Metacritic", "Value": "96/100"}
            //   ]
            JsonNode ratings = response.path("Ratings");
            if (ratings.isArray()) {
                for (JsonNode rating : ratings) {
                    if ("Rotten Tomatoes".equals(rating.path("Source").asText())) {
                        String rtValue = rating.path("Value").asText("");
                        // strip the trailing "%"
                        if (rtValue.endsWith("%")) {
                            try {
                                result.rtRating = Integer.parseInt(
                                        rtValue.substring(0, rtValue.length() - 1));
                            } catch (NumberFormatException ignored) { }
                        }
                        break;
                    }
                }
            }

            return Optional.of(result);

        } catch (Exception e) {
            log.warn("OMDb lookup failed for IMDb id {}: {}", imdbId, e.getMessage());
            return Optional.empty();
        }
    }

    public static class OmdbResult {
        public Double imdbRating;   // 0.0 - 10.0
        public Integer rtRating;    // 0 - 100
    }
}