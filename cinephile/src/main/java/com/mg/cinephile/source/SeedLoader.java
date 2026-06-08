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
import com.mg.cinephile.source.OmdbClient;
import com.mg.cinephile.source.TmdbClient;

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
    private final TmdbClient tmdbClient;
    private final OmdbClient omdbClient;

    public SeedLoader(List<ScreeningSource> sources,
                      TheaterRepository theaterRepository,
                      MovieRepository movieRepository,
                      ScreeningRepository screeningRepository,
                      Classifier classifier,
                      TmdbClient tmdbClient,
                      OmdbClient omdbClient) {
        this.sources = sources;
        this.theaterRepository = theaterRepository;
        this.movieRepository = movieRepository;
        this.screeningRepository = screeningRepository;
        this.classifier = classifier;
        this.tmdbClient = tmdbClient;
        this.omdbClient = omdbClient;
    }

    @Override
    public void run(ApplicationArguments args) {
        ingestAll();
    }

    public void ingestAll(){
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
                    enrichMovie(movie);                   // NEW
                    movie = movieRepository.save(movie);
                }
            } else if (movie.getId() == null) {
                movie = movieRepository.save(movie);
            }
            screening.setMovie(movie);
            screening.setMovie(movie);

            screening.setSource(source.getName());
            screening.setSpecialCategory(classifier.classify(screening));
            screeningRepository.save(screening);
            added++;
        }

        log.info("  added {}, skipped {} (duplicates)", added, skipped);
        return new int[]{added, skipped};
    }

    /**
     * Enrich a freshly-discovered movie with TMDB metadata + OMDb ratings.
     * Modifies the movie in place. Silently does nothing if enrichment fails;
     * the movie still saves with whatever fields it already had from the source.
     */
    private void enrichMovie(Movie movie) {
        // Step 1 — TMDB for metadata (synopsis, poster, country, IMDb id)
        Optional<TmdbClient.TmdbResult> tmdbResult = tmdbClient.enrich(movie.getTitle());
        if (tmdbResult.isEmpty()) return;

        TmdbClient.TmdbResult tmdb = tmdbResult.get();

        // Don't overwrite values that may already exist; fill only blanks
        if (tmdb.synopsis != null) movie.setSynopsis(tmdb.synopsis);
        if (tmdb.posterUrl != null) movie.setPosterUrl(tmdb.posterUrl);
        if (tmdb.countryOfOrigin != null) movie.setCountryOfOrigin(tmdb.countryOfOrigin);
        if (tmdb.tmdbId != null) movie.setTmdbId(tmdb.tmdbId);
        if (tmdb.imdbId != null) movie.setImdbId(tmdb.imdbId);
        if (tmdb.releaseDate != null && movie.getReleaseDate() == null) {
            // Only override release date if AMC didn't provide one
            movie.setReleaseDate(tmdb.releaseDate);
        }

        // Step 2 — OMDb for ratings, only if we have an IMDb id from TMDB
        if (tmdb.imdbId == null) return;

        Optional<OmdbClient.OmdbResult> omdbResult = omdbClient.fetchRatings(tmdb.imdbId);
        if (omdbResult.isEmpty()) return;

        OmdbClient.OmdbResult omdb = omdbResult.get();
        if (omdb.imdbRating != null) movie.setImdbRating(omdb.imdbRating);
        if (omdb.rtRating != null) movie.setRtRating(omdb.rtRating);
    }
}