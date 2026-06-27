package com.be.domain.participant.dto.request;

public record ParticipantCreateRequest(
        Long voteId,
        String deviceToken
) {}
