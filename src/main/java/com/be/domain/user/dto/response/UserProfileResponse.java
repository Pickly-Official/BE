package com.be.domain.user.dto.response;

public record UserProfileResponse(
        Long id,
        String nickname,
        String provider,
        String profileImage
) {}
