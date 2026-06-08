package com.mg.cinephile.classifier;

import com.mg.cinephile.domain.Movie;
import com.mg.cinephile.domain.Screening;
import com.mg.cinephile.domain.SpecialCategory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.util.Set;

@Component
public class Classifier {

    private static final int REPERTORY_AGE_YEARS = 10;

    private static final int ANNIVERSARY_MIN_AGE_YEARS = 20;

    private static final String DOMESTIC_COUNTRY = "US";

    private static final int ANNIVERSARY_INTERVAL = 5;

    private static final Set<String> FESTIVAL_KEYWORDS = Set.of("festival", "fest");
    private static final Set<String> SPECIAL_FORMAT_KEYWORDS = Set.of(
            "imax", "70mm", "4k", "restoration", "anniversary", "classic"
    );

    public SpecialCategory classify(Screening screening) {
        Movie movie = screening.getMovie();
        if (movie == null) {
            return SpecialCategory.REGULAR;
        }

        String text = combinedText(screening, movie);

        if (containsAny(text, FESTIVAL_KEYWORDS)) {
            return SpecialCategory.FESTIVAL;
        }

        LocalDate releaseDate = movie.getReleaseDate();
        if (releaseDate != null) {
            int yearsOld = LocalDate.now().getYear() - releaseDate.getYear();

            // Milestone anniversary (20th, 25th, 30th, ...)
            if (yearsOld >= ANNIVERSARY_MIN_AGE_YEARS && yearsOld % ANNIVERSARY_INTERVAL == 0) {
                return SpecialCategory.ANNIVERSARY;
            }

            // General repertory: just old, not necessarily a milestone
            if (yearsOld >= REPERTORY_AGE_YEARS) {
                return SpecialCategory.REPERTORY;
            }
        }

        String country = movie.getCountryOfOrigin();
        if (country != null && !country.isBlank() && !DOMESTIC_COUNTRY.equalsIgnoreCase(country)) {
            return SpecialCategory.INTERNATIONAL;
        }

        if (containsAny(text, SPECIAL_FORMAT_KEYWORDS)) {
            return SpecialCategory.REPERTORY;
        }

        // Default — nothing special found
        return SpecialCategory.REGULAR;
    }


    private String combinedText(Screening screening, Movie movie) {
        StringBuilder sb = new StringBuilder();
        if (movie.getTitle() != null) sb.append(movie.getTitle()).append(' ');
        if (screening.getFormat() != null) sb.append(screening.getFormat()).append(' ');
        return sb.toString().toLowerCase();
    }

    private boolean containsAny(String text, Set<String> keywords) {
        for (String keyword : keywords) {
            if (text.contains(keyword)) return true;
        }
        return false;
    }
}
