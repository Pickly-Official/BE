package com.be.domain.analysis.repository;

import com.be.domain.analysis.entity.PhotoAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PhotoAnalysisRepository extends JpaRepository<PhotoAnalysis, Long> {
}
