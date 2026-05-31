package com.mg.cinephile.source;

import com.fasterxml.jackson.databind.JsonNode;
import com.mg.cinephile.domain.Movie;
import com.mg.cinephile.domain.Screening;
import com.mg.cinephile.domain.Theater;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;

@Component
public class AmcScreeningSource implements ScreeningSource {

    private static final Logger log = LoggerFactory.getLogger(AmcScreeningSource.class);

    // How many theaters to fetch (paged). Keep small for first pass.
    private static final int THEATER_PAGE_SIZE = 10;

    private final String apiKey;
    private final WebClient webClient;

    public AmcScreeningSource(@Value("${amc.api.key:}") String apiKey,
                              @Value("${amc.api.base-url}") String baseUrl) {
        this.apiKey = apiKey;
        this.webClient = WebClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    @Override
    public String getName() {
        return "amc";
    }

    @Override
    public List<Screening> fetchScreenings() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("AMC API key not set; skipping AMC source.");
            return List.of();
        }

        log.info("Fetching AMC theaters...");
        List<Theater> theaters = fetchTheaters();
        log.info("Got {} AMC theaters.", theaters.size());

        // For each theater, fetch tomorrow's showtimes.
        LocalDate date = LocalDate.now().plusDays(1);
        List<Screening> all = new ArrayList<>();
        for (Theater theater : theaters) {
            try {
                all.addAll(fetchShowtimesForTheater(theater, date));
            } catch (Exception e) {
                log.warn("Failed to fetch showtimes for theater {}: {}",
                        theater.getName(), e.getMessage());
            }
        }

        log.info("Fetched {} AMC screenings.", all.size());
        return all;
    }

    /**
     * GET /v2/theatres?page-size=10
     * Parses the paginated HAL response and pulls out theater info.
     */
    private List<Theater> fetchTheaters() {
        JsonNode response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/theatres")
                        .queryParam("page-size", THEATER_PAGE_SIZE)
                        .build())
                .header("X-AMC-Vendor-Key", apiKey)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        List<Theater> result = new ArrayList<>();
        if (response == null) return result;

        JsonNode theatersNode = response.path("_embedded").path("theatres");
        for (JsonNode node : theatersNode) {
            Theater t = new Theater();
            t.setName(node.path("name").asText());
            t.setCity(node.path("city").asText(""));
            t.setLatitude(node.path("latitude").asDouble(0));
            t.setLongitude(node.path("longitude").asDouble(0));
            // Store AMC's theater id in a transient way via a hack: we'll
            // attach it to the address field for now so the showtimes
            // call can find it. Cleaner approach later.
            t.setExternalId(String.valueOf(node.path("id").asLong()));
            result.add(t);
        }
        return result;
    }

    /**
     * GET /v2/theatres/{id}/showtimes/{MM-DD-YYYY}
     */
    private List<Screening> fetchShowtimesForTheater(Theater theater, LocalDate date) {
        // We stashed the AMC id in the address field above.
        String amcTheaterId = theater.getExternalId();
        String dateStr = date.format(DateTimeFormatter.ofPattern("MM-dd-yyyy"));

        JsonNode response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/theatres/{id}/showtimes/{date}")
                        .build(amcTheaterId, dateStr))
                .header("X-AMC-Vendor-Key", apiKey)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        List<Screening> result = new ArrayList<>();
        if (response == null) return result;

        JsonNode showtimes = response.path("_embedded").path("showtimes");
        for (JsonNode node : showtimes) {
            // Build a minimal Movie from the showtime payload.
            Movie movie = new Movie();
            movie.setTitle(node.path("movieName").asText());

            // showDateTimeUtc looks like "2014-12-15T16:15:00Z"
            String utcStr = node.path("showDateTimeUtc").asText();
            LocalDateTime startTime = LocalDateTime.parse(
                            utcStr.replace("Z", ""))
                    .atZone(ZoneId.of("UTC"))
                    .toLocalDateTime();

            Screening screening = new Screening(
                    movie,
                    theater,
                    startTime,
                    "Standard"
            );
            result.add(screening);
        }
        return result;
    }
}