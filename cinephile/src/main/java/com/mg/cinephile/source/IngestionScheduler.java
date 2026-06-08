package com.mg.cinephile.source;

import com.mg.cinephile.domain.Screening;
import com.mg.cinephile.repository.ScreeningRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Periodically re-ingests AMC data and cleans up screenings whose
 * start time has passed.
 *
 * Times below use cron syntax (second minute hour day month weekday)
 * and are evaluated in the server's local timezone.
 */
@Component
public class IngestionScheduler {

    private static final Logger log = LoggerFactory.getLogger(IngestionScheduler.class);

    private final SeedLoader seedLoader;
    private final ScreeningRepository screeningRepository;

    public IngestionScheduler(SeedLoader seedLoader,
                              ScreeningRepository screeningRepository) {
        this.seedLoader = seedLoader;
        this.screeningRepository = screeningRepository;
    }

    /**
     * Re-ingest from every screening source every 6 hours.
     * fixedRate measures from the *start* of one run to the start of the next.
     * initialDelay waits 10 minutes after startup before the first scheduled
     * run, so the startup ingest doesn't immediately re-fire.
     */
    @Scheduled(fixedRate = 6 * 60 * 60 * 1000L, initialDelay = 10 * 60 * 1000L)
    public void scheduledIngest() {
        log.info("Scheduled ingestion triggered.");
        try {
            seedLoader.ingestAll();
        } catch (Exception e) {
            log.error("Scheduled ingestion failed: {}", e.getMessage(), e);
        }
    }

    /**
     * Delete screenings whose start time has already passed.
     * Runs once a day at 4:00 AM local time (cron: second minute hour ...).
     */
    @Scheduled(cron = "0 0 4 * * *")
    @Transactional
    public void cleanUpPastScreenings() {
        log.info("Past-screening cleanup triggered.");
        List<Screening> past = screeningRepository.findByStartTimeBefore(LocalDateTime.now());
        int count = past.size();
        screeningRepository.deleteAll(past);
        log.info("Deleted {} past screenings.", count);
    }
}