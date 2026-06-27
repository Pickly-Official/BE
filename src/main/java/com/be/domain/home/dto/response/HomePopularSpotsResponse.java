package com.be.domain.home.dto.response;

import java.util.List;

public record HomePopularSpotsResponse(List<SpotItem> popularSpots) {

    public record SpotItem(
            int rank,
            String name,
            int recommendRatio,
            List<String> keywords
    ) {}
}
