package com.be.domain.participant.dto.response;

public record ParticipantResponse(
        Long participantId,
        Long voteId,
        String voterUuid,
        boolean completed
) {}
