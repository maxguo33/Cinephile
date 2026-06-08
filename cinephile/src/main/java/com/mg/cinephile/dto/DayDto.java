package com.mg.cinephile.dto;

import java.time.LocalDate;
import java.util.List;

public class DayDto {

    private LocalDate date;
    private List<ScreeningDto> screenings;

    public DayDto() {
    }

    public DayDto(LocalDate date, List<ScreeningDto> screenings) {
        this.date = date;
        this.screenings = screenings;
    }

    public LocalDate getDate() { return date; }
    public void setDate(LocalDate date) { this.date = date; }

    public List<ScreeningDto> getScreenings() { return screenings; }
    public void setScreenings(List<ScreeningDto> screenings) { this.screenings = screenings; }
}