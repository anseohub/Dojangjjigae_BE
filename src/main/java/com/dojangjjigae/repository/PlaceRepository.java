package com.dojangjjigae.repository;

import com.dojangjjigae.domain.Place;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PlaceRepository extends JpaRepository<Place, Long> {
    Optional<Place> findBySlug(String slug);
}
