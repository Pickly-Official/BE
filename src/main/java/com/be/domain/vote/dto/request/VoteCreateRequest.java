package com.be.domain.vote.dto.request;

public record VoteCreateRequest(
        String title,
        boolean useLocation,
        String deadlineType
) {}
