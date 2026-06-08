package com.mg.cinephile.controller;

import com.mg.cinephile.dto.CalendarDto;
import com.mg.cinephile.service.CalendarService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/calendar")
public class CalendarController {

    private final CalendarService calendarService;

    public CalendarController(CalendarService calendarService) {
        this.calendarService = calendarService;
    }

    // GET /api/calendar?lat=...&lng=...&radiusKm=...&month=2026-06
    @GetMapping
    public CalendarDto getCalendar(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "50") double radiusKm,
            @RequestParam String month) {
        return calendarService.getCalendarForMonth(month, lat, lng, radiusKm);
    }

    // GET /api/calendar/upcoming?lat=...&lng=...&radiusKm=...
    @GetMapping("/upcoming")
    public CalendarDto getUpcoming(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "50") double radiusKm) {
        return calendarService.getUpcoming(lat, lng, radiusKm);
    }
}