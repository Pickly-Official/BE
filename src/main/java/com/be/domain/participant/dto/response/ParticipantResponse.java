package com.be.domain.participant.dto.response;

public record ParticipantResponse(
        Long participantId,
        Boolean alreadyJoined
) {}
