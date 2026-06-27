package com.be.domain.vote.dto.response;

public record VoteCreateResponse(
        Long voteId,
        String title,
        String deadlineType,
        String closedAt
) {}
