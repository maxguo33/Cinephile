package com.mg.cinephile.repository;

import com.mg.cinephile.domain.Theater;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface TheaterRepository extends JpaRepository<Theater, Long> {

    Optional<Theater> findByExternalId(String externalID);
}
