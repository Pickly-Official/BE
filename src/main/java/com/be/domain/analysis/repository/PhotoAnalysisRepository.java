package com.be.domain.analysis.repository;

import com.be.domain.analysis.entity.PhotoAnalysis;
import com.be.domain.photo.entity.Photo;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface PhotoAnalysisRepository extends JpaRepository<PhotoAnalysis, Long> {
    Optional<PhotoAnalysis> findByPhoto(Photo photo);
}
