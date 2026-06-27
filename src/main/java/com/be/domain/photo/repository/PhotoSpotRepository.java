package com.be.domain.photo.repository;

import com.be.domain.photo.entity.PhotoSpot;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PhotoSpotRepository extends JpaRepository<PhotoSpot, Long> {
    Optional<PhotoSpot> findByName(String name);
}
