package com.be.domain.participant.repository;

import com.be.domain.participant.entity.Swipe;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SwipeRepository extends JpaRepository<Swipe, Long> {
}
