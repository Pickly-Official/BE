package com.be.domain.participant.dto.request;

import com.be.domain.participant.entity.SwipeChoice;

public record SwipeRequest(
        Long photoId,
        SwipeChoice choice
) {}
