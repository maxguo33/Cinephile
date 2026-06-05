package com.mg.cinephile.controller;

import com.mg.cinephile.dto.ScreeningDto;
import com.mg.cinephile.service.ScreeningService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/screenings")
public class ScreeningController {

    private final ScreeningService screeningService;

    public ScreeningController(ScreeningService screeningService) {
        this.screeningService = screeningService;
    }

    // GET /api/screenings
    @GetMapping
    public List<ScreeningDto> getAllScreenings() {
        return screeningService.getAllScreenings();
    }

    // GET /api/screenings/{id}
    @GetMapping("/{id}")
    public ScreeningDto getScreeningById(@PathVariable Long id) {
        return screeningService.getScreeningById(id);
    }

    // POST /api/screenings
    @PostMapping
    public ScreeningDto createScreening(@RequestBody ScreeningDto dto) {
        return screeningService.createScreening(dto);
    }

    @GetMapping("/near")
    public List<ScreeningDto> findNear(
            @RequestParam double lat,
            @RequestParam double lng,
            @RequestParam(defaultValue = "10") double radiusKm) {
        return screeningService.findScreeningsNear(lat, lng, radiusKm);
    }

}