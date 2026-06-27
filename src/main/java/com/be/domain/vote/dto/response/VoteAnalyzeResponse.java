package com.be.domain.vote.dto.response;

import java.util.List;

public record VoteAnalyzeResponse(
        String summary,
        List<String> keywords,
        String modelName
) {}
