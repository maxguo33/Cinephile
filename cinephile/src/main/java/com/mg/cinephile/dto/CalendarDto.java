package com.mg.cinephile.dto;

import java.util.List;

public class CalendarDto {

    private String month;          // "2026-06"
    private double lat;
    private double lng;
    private double radiusKm;
    private List<DayDto> days;

    public CalendarDto() {
    }

    public CalendarDto(String month, double lat, double lng, double radiusKm, List<DayDto> days) {
        this.month = month;
        this.lat = lat;
        this.lng = lng;
        this.radiusKm = radiusKm;
        this.days = days;
    }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }

    public double getLat() { return lat; }
    public void setLat(double lat) { this.lat = lat; }

    public double getLng() { return lng; }
    public void setLng(double lng) { this.lng = lng; }

    public double getRadiusKm() { return radiusKm; }
    public void setRadiusKm(double radiusKm) { this.radiusKm = radiusKm; }

    public List<DayDto> getDays() { return days; }
    public void setDays(List<DayDto> days) { this.days = days; }
}