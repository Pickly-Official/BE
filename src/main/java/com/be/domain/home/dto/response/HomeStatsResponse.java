package com.be.domain.home.dto.response;

import lombok.AllArgsConstructor;
import lombok.Getter;

@Getter
@AllArgsConstructor
public class HomeStatsResponse {
    private Long totalParticipants;
    private Long todayVoteCount;
}
