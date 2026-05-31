package com.mg.cinephile.dto;

import java.time.LocalDate;

public class MovieDto {

    private Long id;
    private String title;
    private LocalDate releaseDate;
    private String synopsis;
    private Double imdbRating;

    public MovieDto() {
    }

    public MovieDto(Long id, String title, LocalDate releaseDate,
                    String synopsis, Double imdbRating) {
        this.id = id;
        this.title = title;
        this.releaseDate = releaseDate;
        this.synopsis = synopsis;
        this.imdbRating = imdbRating;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public LocalDate getReleaseDate() { return releaseDate; }
    public void setReleaseDate(LocalDate releaseDate) { this.releaseDate = releaseDate; }

    public String getSynopsis() { return synopsis; }
    public void setSynopsis(String synopsis) { this.synopsis = synopsis; }

    public Double getImdbRating() { return imdbRating; }
    public void setImdbRating(Double imdbRating) { this.imdbRating = imdbRating; }
}