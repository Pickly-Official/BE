package com.be.domain.participant.repository;

import com.be.domain.participant.entity.Swipe;
import com.be.domain.participant.entity.SwipeChoice;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SwipeRepository extends JpaRepository<Swipe, Long> {
    long countByPhotoIdAndChoice(Long photoId, SwipeChoice choice);
}
