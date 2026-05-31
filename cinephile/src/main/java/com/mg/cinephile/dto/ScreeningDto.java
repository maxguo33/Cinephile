package com.mg.cinephile.dto;

import java.time.LocalDateTime;

public class ScreeningDto {

    private Long id;
    private MovieDto movie;
    private TheaterDto theater;
    private LocalDateTime startTime;
    private String format;

    public ScreeningDto() {
    }

    public ScreeningDto(Long id, MovieDto movie, TheaterDto theater,
                        LocalDateTime startTime, String format) {
        this.id = id;
        this.movie = movie;
        this.theater = theater;
        this.startTime = startTime;
        this.format = format;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public MovieDto getMovie() { return movie; }
    public void setMovie(MovieDto movie) { this.movie = movie; }

    public TheaterDto getTheater() { return theater; }
    public void setTheater(TheaterDto theater) { this.theater = theater; }

    public LocalDateTime getStartTime() { return startTime; }
    public void setStartTime(LocalDateTime startTime) { this.startTime = startTime; }

    public String getFormat() { return format; }
    public void setFormat(String format) { this.format = format; }
}