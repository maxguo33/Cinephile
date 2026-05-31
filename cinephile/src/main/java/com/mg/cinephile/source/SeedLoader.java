package com.mg.cinephile.source;

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

@Component
public class SeedLoader implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(SeedLoader.class);

    private final List<ScreeningSource> sources;
    private final TheaterRepository theaterRepository;
    private final MovieRepository movieRepository;
    private final ScreeningRepository screeningRepository;

    public SeedLoader(List<ScreeningSource> sources,
                      TheaterRepository theaterRepository,
                      MovieRepository movieRepository,
                      ScreeningRepository screeningRepository) {
        this.sources = sources;
        this.theaterRepository = theaterRepository;
        this.movieRepository = movieRepository;
        this.screeningRepository = screeningRepository;
    }

    @Override
    public void run(ApplicationArguments args) {
        // Don't reseed if data already exists
        if (screeningRepository.count() > 0) {
            log.info("Data already present, skipping seed.");
            return;
        }

        log.info("Found {} screening source(s): {}",
                sources.size(),
                sources.stream().map(ScreeningSource::getName).toList());

        for (ScreeningSource source : sources) {
            try {
                loadFromSource(source);
            } catch (Exception e) {
                log.error("Failed to load from source '{}': {}",
                        source.getName(), e.getMessage(), e);
            }
        }

        log.info("Seed complete. Theaters: {}, Movies: {}, Screenings: {}",
                theaterRepository.count(),
                movieRepository.count(),
                screeningRepository.count());
    }

    private void loadFromSource(ScreeningSource source) {
        log.info("Loading from source: {}", source.getName());

        List<Screening> screenings = source.fetchScreenings();
        log.info("  fetched {} screenings", screenings.size());

        for (Screening screening : screenings) {
            // Persist the theater first (if it isn't already)
            Theater theater = screening.getTheater();
            if (theater.getId() == null) {
                theater = theaterRepository.save(theater);
                screening.setTheater(theater);
            }

            // Then the movie
            Movie movie = screening.getMovie();
            if (movie.getId() == null) {
                movie = movieRepository.save(movie);
                screening.setMovie(movie);
            }

            // Mark which source this came from
            screening.se tSource(source.getName());

            screeningRepository.save(screening);
        }
    }
}