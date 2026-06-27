package com.be.domain.participant.dto.response;

public record ParticipantResponse(
        Long participantId,
        Long voteId,
        String deviceToken,
        boolean completed
) {}
