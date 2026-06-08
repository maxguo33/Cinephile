package com.mg.cinephile.source;

import com.mg.cinephile.classifier.Classifier;
import com.mg.cinephile.domain.Movie;
import com.mg.cinephile.domain.Screening;
import com.mg.cinephile.domain.Theater;
import com.mg.cinephile.repository.MovieRepository;
import com.mg.cinephile.repository.ScreeningRepository;
import com.mg.cinephile.repository.TheaterRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Optional;

@Component
public class SeedLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedLoader.class);

    private final List<ScreeningSource> sources;
    private final TheaterRepository theaterRepository;
    private final MovieRepository movieRepository;
    private final ScreeningRepository screeningRepository;
    private final Classifier classifier;

    public SeedLoader(List<ScreeningSource> sources,
                      TheaterRepository theaterRepository,
                      MovieRepository movieRepository,
                      ScreeningRepository screeningRepository,
                      Classifier classifier) {
        this.sources = sources;
        this.theaterRepository = theaterRepository;
        this.movieRepository = movieRepository;
        this.screeningRepository = screeningRepository;
        this.classifier = classifier;
    }

    @Override
    public void run(ApplicationArguments args) {
        log.info("Found {} screening source(s): {}",
                sources.size(),
                sources.stream().map(ScreeningSource::getName).toList());

        int totalAdded = 0;
        int totalSkipped = 0;

        for (ScreeningSource source : sources) {
            try {
                int[] counts = loadFromSource(source);
                totalAdded += counts[0];
                totalSkipped += counts[1];
            } catch (Exception e) {
                log.error("Failed to load from source '{}': {}",
                        source.getName(), e.getMessage(), e);
            }
        }

        log.info("Ingestion complete. Added: {}, Skipped (duplicate): {}. Totals — Theaters: {}, Movies: {}, Screenings: {}",
                totalAdded, totalSkipped,
                theaterRepository.count(),
                movieRepository.count(),
                screeningRepository.count());
    }

    /**
     * @return [added, skipped]
     */
    private int[] loadFromSource(ScreeningSource source) {
        log.info("Loading from source: {}", source.getName());

        List<Screening> screenings = source.fetchScreenings();
        log.info("  fetched {} screenings", screenings.size());

        int added = 0;
        int skipped = 0;

        for (Screening screening : screenings) {
            // Skip if we already have this screening (by externalId)
            if (screening.getExternalId() != null) {
                if (screeningRepository.findByExternalId(screening.getExternalId()).isPresent()) {
                    skipped++;
                    continue;
                }
            }

            // Reuse existing theater if we've already saved one with this external id
            Theater theater = screening.getTheater();
            if (theater.getExternalId() != null) {
                Optional<Theater> existing = theaterRepository.findByExternalId(theater.getExternalId());
                if (existing.isPresent()) {
                    theater = existing.get();
                } else {
                    theater = theaterRepository.save(theater);
                }
            } else if (theater.getId() == null) {
                theater = theaterRepository.save(theater);
            }
            screening.setTheater(theater);

            // Reuse existing movie if we've already saved one with this title
            Movie movie = screening.getMovie();
            if (movie.getTitle() != null) {
                Optional<Movie> existing = movieRepository.findByTitle(movie.getTitle());
                if (existing.isPresent()) {
                    movie = existing.get();
                } else {
                    movie = movieRepository.save(movie);
                }
            } else if (movie.getId() == null) {
                movie = movieRepository.save(movie);
            }
            screening.setMovie(movie);

            screening.setSource(source.getName());
            screening.setSpecialCategory(classifier.classify(screening));
            screeningRepository.save(screening);
            added++;
        }

        log.info("  added {}, skipped {} (duplicates)", added, skipped);
        return new int[]{added, skipped};
    }
}