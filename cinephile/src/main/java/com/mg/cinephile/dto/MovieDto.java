package com.mg.cinephile.dto;

import java.time.LocalDate;

public class MovieDto {

    private Long id;
    private String title;
    private LocalDate releaseDate;
    private String synopsis;
    private Double imdbRating;

    // NEW — fields populated by TMDB + OMDb enrichment
    private Integer rtRating;
    private String posterUrl;
    private String countryOfOrigin;
    private String tmdbId;
    private String imdbId;

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

    public Integer getRtRating() { return rtRating; }
    public void setRtRating(Integer rtRating) { this.rtRating = rtRating; }

    public String getPosterUrl() { return posterUrl; }
    public void setPosterUrl(String posterUrl) { this.posterUrl = posterUrl; }

    public String getCountryOfOrigin() { return countryOfOrigin; }
    public void setCountryOfOrigin(String countryOfOrigin) { this.countryOfOrigin = countryOfOrigin; }

    public String getTmdbId() { return tmdbId; }
    public void setTmdbId(String tmdbId) { this.tmdbId = tmdbId; }

    public String getImdbId() { return imdbId; }
    public void setImdbId(String imdbId) { this.imdbId = imdbId; }
}