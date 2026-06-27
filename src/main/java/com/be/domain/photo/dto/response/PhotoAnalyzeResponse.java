package com.be.domain.photo.dto.response;

public record PhotoAnalyzeResponse(
        Long photoId,
        String composition,
        String expression,
        String lighting,
        String color,
        String background
) {}
