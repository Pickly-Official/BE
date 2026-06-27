package com.be.domain.participant.dto.request;

public record SwipeRequest(
        Long participantId,
        Long photoId,
        String choice
) {}
