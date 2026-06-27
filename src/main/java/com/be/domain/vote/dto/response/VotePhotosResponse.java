package com.be.domain.vote.dto.response;

import java.util.List;

public record VotePhotosResponse(
        Long voteId,
        String title,
        List<PhotoInfo> photos
) {
    public record PhotoInfo(
            Long photoId,
            String imageUrl,
            Integer sequence
    ) {}
}
