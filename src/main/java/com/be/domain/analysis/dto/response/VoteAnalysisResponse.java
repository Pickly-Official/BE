package com.be.domain.analysis.dto.response;

import java.util.List;

public record VoteAnalysisResponse(
        Long analysisId,
        Long voteId,
        String status,
        String summary,
        List<String> keywords
) {}
