package com.mg.cinephile.controller;

import com.mg.cinephile.dto.MovieDto;
import com.mg.cinephile.service.MovieService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/movies")
public class MovieController {

    private final MovieService movieService;

    public MovieController(MovieService movieService) {
        this.movieService = movieService;
    }

    // GET /api/movies
    @GetMapping
    public List<MovieDto> getAllMovies() {
        return movieService.getAllMovies();
    }

    // GET /api/movies/{id}
    @GetMapping("/{id}")
    public MovieDto getMovieById(@PathVariable Long id) {
        return movieService.getMovieById(id);
    }

    // POST /api/movies
    @PostMapping
    public MovieDto createMovie(@RequestBody MovieDto dto) {
        return movieService.createMovie(dto);
    }
}