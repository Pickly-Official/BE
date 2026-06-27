package com.be.domain.participant.repository;

import com.be.domain.participant.entity.Participant;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    long countByVoteId(Long voteId);

    Optional<Participant> findByVoteIdAndVoterUuid(Long voteId, String voterUuid);
}
