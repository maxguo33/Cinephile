package com.mg.cinephile.controller;

import com.mg.cinephile.dto.TheaterDto;
import com.mg.cinephile.service.TheaterService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/theaters")
public class TheaterController {

    private final TheaterService theaterService;

    public TheaterController(TheaterService theaterService) {
        this.theaterService = theaterService;
    }

    // GET /api/theaters
    @GetMapping
    public List<TheaterDto> getAllTheaters() {
        return theaterService.getAllTheaters();
    }

    // GET /api/theaters/{id}
    @GetMapping("/{id}")
    public TheaterDto getTheaterById(@PathVariable Long id) {
        return theaterService.getTheaterById(id);
    }

    // POST /api/theaters
    @PostMapping
    public TheaterDto createTheater(@RequestBody TheaterDto dto) {
        return theaterService.createTheater(dto);
    }
}