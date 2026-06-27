package com.be.domain.photo.repository;

import com.be.domain.photo.entity.PhotoSpot;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoSpotRepository extends JpaRepository<PhotoSpot, Long> {
}
