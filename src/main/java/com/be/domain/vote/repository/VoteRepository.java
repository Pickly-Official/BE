package com.be.domain.vote.repository;

import com.be.domain.vote.entity.Vote;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDateTime;
import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {
    long countByCreatedAtBetween(LocalDateTime start, LocalDateTime end);
    long countByUserId(Long userId);

    List<Vote> findByUserId(Long userId);
}
