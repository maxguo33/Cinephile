package com.mg.cinephile.service;

import com.mg.cinephile.domain.Theater;
import com.mg.cinephile.dto.TheaterDto;
import com.mg.cinephile.repository.TheaterRepository;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class TheaterService {

    private final TheaterRepository theaterRepository;

    public TheaterService(TheaterRepository theaterRepository) {
        this.theaterRepository = theaterRepository;
    }

    // --- read all theaters ---
    public List<TheaterDto> getAllTheaters() {
        return theaterRepository.findAll()
                .stream()
                .map(this::toDto)
                .toList();
    }

    // --- read one theater by id ---
    public TheaterDto getTheaterById(Long id) {
        Theater theater = theaterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Theater not found: " + id));
        return toDto(theater);
    }

    // --- create a new theater ---
    public TheaterDto createTheater(TheaterDto dto) {
        Theater theater = toEntity(dto);
        Theater saved = theaterRepository.save(theater);
        return toDto(saved);
    }

    // --- conversion: entity -> dto ---
    private TheaterDto toDto(Theater theater) {
        return new TheaterDto(
                theater.getId(),
                theater.getName(),
                theater.getCity(),
                theater.getLatitude(),
                theater.getLongitude()
        );
    }

    // --- conversion: dto -> entity ---
    private Theater toEntity(TheaterDto dto) {
        return new Theater(
                dto.getName(),
                dto.getCity(),
                dto.getLatitude(),
                dto.getLongitude()
        );
    }
}