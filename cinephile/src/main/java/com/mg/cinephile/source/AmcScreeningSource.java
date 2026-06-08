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

    // How many days ahead to fetch showtimes for.
// AMC typically publishes ~2 weeks out, sometimes more.
    private static final int DAYS_AHEAD_TO_FETCH = 14;

    @Override
    public List<Screening> fetchScreenings() {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("AMC API key not set; skipping AMC source.");
            return List.of();
        }

        log.info("Fetching AMC theaters...");
        List<Theater> theaters = fetchTheaters();
        log.info("Got {} AMC theaters.", theaters.size());

        LocalDate startDate = LocalDate.now();
        LocalDate endDate = startDate.plusDays(DAYS_AHEAD_TO_FETCH);
        log.info("Fetching showtimes from {} to {}", startDate, endDate);

        List<Screening> all = new ArrayList<>();
        for (Theater theater : theaters) {
            for (LocalDate date = startDate; !date.isAfter(endDate); date = date.plusDays(1)) {
                try {
                    List<Screening> dayScreenings = fetchShowtimesForTheater(theater, date);
                    all.addAll(dayScreenings);
                } catch (Exception e) {
                    log.warn("Failed to fetch showtimes for theater {} on {}: {}",
                            theater.getName(), date, e.getMessage());
                }
            }
        }

        log.info("Fetched {} AMC screenings across {} days.", all.size(), DAYS_AHEAD_TO_FETCH + 1);
        return all;
    }

    /**
     * GET /v2/theatres?page-size=10
     * Parses the paginated HAL response and pulls out theater info.
     */
    private List<Theater> fetchTheaters() {
        JsonNode response = webClient.get()
                .uri(uriBuilder -> uriBuilder
                        .path("/v2/locations")
                        .queryParam("latitude", 37.33)
                        .queryParam("longitude", -121.89)
                        .queryParam("page-size", THEATER_PAGE_SIZE)
                        .build())
                .header("X-AMC-Vendor-Key", apiKey)
                .retrieve()
                .bodyToMono(JsonNode.class)
                .block();

        List<Theater> result = new ArrayList<>();
        if (response == null) return result;

        JsonNode locations = response.path("_embedded").path("locations");
        for (JsonNode locationNode : locations) {
            JsonNode theatreNode = locationNode.path("_embedded").path("theatre");

            Theater t = new Theater();
            t.setName(theatreNode.path("name").asText());

            // The detailed address is nested at .location inside the theatre
            JsonNode loc = theatreNode.path("location");
            t.setCity(loc.path("city").asText(""));
            t.setLatitude(loc.path("latitude").asDouble(0));
            t.setLongitude(loc.path("longitude").asDouble(0));

            t.setExternalId(String.valueOf(theatreNode.path("id").asLong()));
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
            screening.setExternalId("amc-" + node.path("id").asLong());
            result.add(screening);
        }
        return result;
    }
}