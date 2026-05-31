package com.mg.cinephile.repository;

import com.mg.cinephile.domain.Screening;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ScreeningRepository extends JpaRepository<Screening, Long> {
}