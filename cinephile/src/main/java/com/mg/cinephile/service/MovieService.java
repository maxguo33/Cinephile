package com.mg.cinephile.service;

import com.mg.cinephile.domain.Movie;
import com.mg.cinephile.dto.MovieDto;
import com.mg.cinephile.repository.MovieRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class MovieService {

    private final MovieRepository movieRepository;

    public MovieService(MovieRepository movieRepository) {
        this.movieRepository = movieRepository;
    }

    public List<MovieDto> getAllMovies() {
        return movieRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    public MovieDto getMovieById(Long id) {
        Movie movie = movieRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Movie not found: " + id));
        return toDto(movie);
    }

    public MovieDto createMovie(MovieDto dto) {
        Movie movie = toEntity(dto);
        Movie saved = movieRepository.save(movie);
        return toDto(saved);
    }

    private MovieDto toDto(Movie movie) {
        return new MovieDto(
                movie.getId(),
                movie.getTitle(),
                movie.getReleaseDate(),
                movie.getSynopsis(),
                movie.getImdbRating()
        );
    }

    private Movie toEntity(MovieDto dto) {
        return new Movie(
                dto.getTitle(),
                dto.getReleaseDate(),
                dto.getSynopsis(),
                dto.getImdbRating()
        );
    }
}