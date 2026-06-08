package com.mg.cinephile.service;

import com.mg.cinephile.domain.Movie;
import com.mg.cinephile.domain.Screening;
import com.mg.cinephile.domain.SpecialCategory;
import com.mg.cinephile.domain.Theater;
import com.mg.cinephile.dto.CalendarDto;
import com.mg.cinephile.dto.DayDto;
import com.mg.cinephile.dto.MovieDto;
import com.mg.cinephile.dto.ScreeningDto;
import com.mg.cinephile.dto.TheaterDto;
import com.mg.cinephile.repository.ScreeningRepository;
import com.mg.cinephile.util.DistanceUtil;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

@Service
public class CalendarService {

    private final ScreeningRepository screeningRepository;

    public CalendarService(ScreeningRepository screeningRepository) {
        this.screeningRepository = screeningRepository;
    }

    /**
     * Build a calendar for a specific month.
     * Format of month: "yyyy-MM" (e.g. "2026-06").
     */
    @Transactional(readOnly = true)
    public CalendarDto getCalendarForMonth(String monthString,
                                           double lat, double lng, double radiusKm) {
        YearMonth ym = YearMonth.parse(monthString);
        LocalDateTime from = ym.atDay(1).atStartOfDay();
        LocalDateTime to = ym.atEndOfMonth().atTime(23, 59, 59);
        return buildCalendar(monthString, lat, lng, radiusKm, from, to);
    }

    /**
     * Build a calendar for the next 30 days, starting today.
     */
    @Transactional(readOnly = true)
    public CalendarDto getUpcoming(double lat, double lng, double radiusKm) {
        LocalDateTime from = LocalDateTime.now();
        LocalDateTime to = from.plusDays(30);
        String label = String.format("%s..%s",
                from.toLocalDate(), to.toLocalDate());
        return buildCalendar(label, lat, lng, radiusKm, from, to);
    }

    private CalendarDto buildCalendar(String label,
                                      double lat, double lng, double radiusKm,
                                      LocalDateTime from, LocalDateTime to) {

        // Pull special screenings in the time window
        List<Screening> screenings = screeningRepository
                .findByStartTimeBetweenAndSpecialCategoryNot(from, to, SpecialCategory.REGULAR);

        // Filter to those within the user's radius and build DTOs
        // grouped by date. TreeMap keeps days in chronological order.
        Map<LocalDate, List<ScreeningDto>> byDay = new TreeMap<>();

        for (Screening s : screenings) {
            Theater t = s.getTheater();
            double distance = DistanceUtil.haversineKm(lat, lng, t.getLatitude(), t.getLongitude());
            if (distance > radiusKm) continue;

            ScreeningDto dto = toDto(s);
            dto.setDistanceKm(distance);

            LocalDate date = s.getStartTime().toLocalDate();
            byDay.computeIfAbsent(date, d -> new ArrayList<>()).add(dto);
        }

        // Sort screenings within each day by start time
        for (List<ScreeningDto> list : byDay.values()) {
            list.sort(Comparator.comparing(ScreeningDto::getStartTime));
        }

        // Build day DTOs in chronological order
        List<DayDto> days = new ArrayList<>();
        for (Map.Entry<LocalDate, List<ScreeningDto>> entry : byDay.entrySet()) {
            days.add(new DayDto(entry.getKey(), entry.getValue()));
        }

        return new CalendarDto(label, lat, lng, radiusKm, days);
    }

    /**
     * Local copy of the entity-to-DTO conversion so this service is
     * self-contained. Duplicates logic in ScreeningService.toDto on
     * purpose for v1; we'd extract a shared mapper if the codebase grew.
     */
    private ScreeningDto toDto(Screening screening) {
        Movie m = screening.getMovie();
        Theater t = screening.getTheater();

        MovieDto movieDto = new MovieDto(
                m.getId(), m.getTitle(), m.getReleaseDate(),
                m.getSynopsis(), m.getImdbRating()
        );

        TheaterDto theaterDto = new TheaterDto(
                t.getId(), t.getName(), t.getCity(),
                t.getLatitude(), t.getLongitude()
        );

        ScreeningDto dto = new ScreeningDto(
                screening.getId(),
                movieDto,
                theaterDto,
                screening.getStartTime(),
                screening.getFormat()
        );
        dto.setSpecialCategory(screening.getSpecialCategory());
        return dto;
    }
}