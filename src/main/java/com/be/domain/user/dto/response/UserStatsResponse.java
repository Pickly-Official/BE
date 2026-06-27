package com.be.domain.user.dto.response;

public record UserStatsResponse(
        Long userId,
        int voteCount,
        int participantCount
) {}
