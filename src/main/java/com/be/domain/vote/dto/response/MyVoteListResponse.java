package com.be.domain.vote.dto.response;

import java.util.List;

public record MyVoteListResponse(
        List<VoteItem> votes
) {
    public record VoteItem(
            Long voteId,
            String title,
            String dDay,
            boolean closed
    ) {}
}
