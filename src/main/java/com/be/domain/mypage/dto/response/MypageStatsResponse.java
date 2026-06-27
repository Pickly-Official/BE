package com.be.domain.mypage.dto.response;

public record MypageStatsResponse(
        String nickname,
        String provider,
        long voteCount,
        long receivedCount
) {}
