package com.mg.cinephile.service;

import com.mg.cinephile.domain.Movie;
import com.mg.cinephile.domain.Screening;
import com.mg.cinephile.domain.Theater;
import com.mg.cinephile.dto.MovieDto;
import com.mg.cinephile.dto.ScreeningDto;
import com.mg.cinephile.dto.TheaterDto;
import com.mg.cinephile.repository.MovieRepository;
import com.mg.cinephile.repository.ScreeningRepository;
import com.mg.cinephile.repository.TheaterRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ScreeningService {

    private final ScreeningRepository screeningRepository;
    private final MovieRepository movieRepository;
    private final TheaterRepository theaterRepository;

    public ScreeningService(ScreeningRepository screeningRepository,
                            MovieRepository movieRepository,
                            TheaterRepository theaterRepository) {
        this.screeningRepository = screeningRepository;
        this.movieRepository = movieRepository;
        this.theaterRepository = theaterRepository;
    }

    // --- read all screenings ---
    public List<ScreeningDto> getAllScreenings() {
        return screeningRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    // --- read one screening by id ---
    public ScreeningDto getScreeningById(Long id) {
        Screening screening = screeningRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Screening not found: " + id));
        return toDto(screening);
    }

    // --- create a new screening ---
    public ScreeningDto createScreening(ScreeningDto dto) {
        // Look up the movie and theater by id; they must already exist.
        Long movieId = dto.getMovie().getId();
        Long theaterId = dto.getTheater().getId();

        Movie movie = movieRepository.findById(movieId)
                .orElseThrow(() -> new RuntimeException("Movie not found: " + movieId));
        Theater theater = theaterRepository.findById(theaterId)
                .orElseThrow(() -> new RuntimeException("Theater not found: " + theaterId));

        Screening screening = new Screening(
                movie,
                theater,
                dto.getStartTime(),
                dto.getFormat()
        );

        Screening saved = screeningRepository.save(screening);
        return toDto(saved);
    }

    // --- conversion: entity -> dto ---
    private ScreeningDto toDto(Screening screening) {
        Movie m = screening.getMovie();
        Theater t = screening.getTheater();

        MovieDto movieDto = new MovieDto(
                m.getId(),
                m.getTitle(),
                m.getReleaseDate(),
                m.getSynopsis(),
                m.getImdbRating()
        );

        TheaterDto theaterDto = new TheaterDto(
                t.getId(),
                t.getName(),
                t.getCity(),
                t.getLatitude(),
                t.getLongitude()
        );

        return new ScreeningDto(
                screening.getId(),
                movieDto,
                theaterDto,
                screening.getStartTime(),
                screening.getFormat()
        );
    }
}