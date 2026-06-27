package com.be.domain.vote.dto.response;

import java.time.LocalDateTime;

public record VoteCreateResponse(
        Long voteId,
        String title,
        LocalDateTime closedAt
) {}
