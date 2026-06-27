package com.be.domain.user.dto.request;

public record LoginRequest(
        String provider,
        String providerId,
        String email,
        String nickname
) {}
