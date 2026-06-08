package com.mg.cinephile.repository;

import com.mg.cinephile.domain.Screening;
import com.mg.cinephile.domain.Theater;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface ScreeningRepository extends JpaRepository<Screening, Long> {

    List<Screening> findByTheaterId(Long theaterId);
    Optional<Screening> findByExternalId(String externalId);
    }
