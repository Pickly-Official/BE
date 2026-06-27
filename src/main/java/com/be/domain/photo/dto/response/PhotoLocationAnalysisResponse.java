package com.be.domain.photo.dto.response;

import java.util.List;

public record PhotoLocationAnalysisResponse(
        List<LocationGroup> groups,
        List<Integer> missingGpsSequences
) {
    public record LocationGroup(
            String name,
            List<Integer> sequences,
            Double latitude,
            Double longitude
    ) {}
}
