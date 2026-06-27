package com.be.domain.vote.dto.response;

import java.util.List;

public record VoteResultResponse(
        Long voteId,
        String title,
        String spotName,
        Integer participantCount,
        List<PhotoRankInfo> photos
) {
    public record PhotoRankInfo(
            Long photoId,
            Integer sequence,
            String imageUrl,
            Integer recommendRate,
            Integer rank,
            PhotoAnalysisInfo analysis
    ) {}

    public record PhotoAnalysisInfo(
            String type,
            String composition,
            String expression,
            String lighting,
            String mood
    ) {}
}
