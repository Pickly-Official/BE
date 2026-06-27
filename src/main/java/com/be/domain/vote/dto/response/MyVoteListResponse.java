package com.be.domain.vote.dto.response;

import java.util.List;

public record MyVoteListResponse(
        List<VoteInfo> inProgress,
        List<VoteInfo> closed
) {
    public record VoteInfo(
            Long voteId,
            String title,
            Integer participantCount,
            String dDay
    ) {}
}
