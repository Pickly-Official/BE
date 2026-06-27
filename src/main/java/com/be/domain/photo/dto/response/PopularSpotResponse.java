package com.be.domain.photo.dto.response;

import java.math.BigDecimal;
import java.util.List;

public record PopularSpotResponse(
        Long spotId,
        String name,
        BigDecimal latitude,
        BigDecimal longitude,
        int photoCount,
        List<String> keywords
) {}
