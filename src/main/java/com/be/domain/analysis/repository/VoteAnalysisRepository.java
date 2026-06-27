package com.be.domain.analysis.repository;

import com.be.domain.analysis.entity.VoteAnalysis;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface VoteAnalysisRepository extends JpaRepository<VoteAnalysis, Long> {
    Optional<VoteAnalysis> findByVoteId(Long voteId);
}
