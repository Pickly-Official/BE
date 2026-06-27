package com.be.domain.participant.repository;

import com.be.domain.participant.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    long countByVoteId(Long voteId);
}
