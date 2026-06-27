package com.be.domain.participant.dto.response;

public record SwipeResponse(
        Long swipeId,
        Long photoId,
        String choice
) {}
