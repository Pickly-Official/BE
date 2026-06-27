package com.be.domain.home.controller;

import com.be.domain.home.dto.response.HomePopularSpotsResponse;
import com.be.domain.home.dto.response.HomeStatsResponse;
import com.be.domain.home.service.HomeService;
import com.be.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Home", description = "홈 화면 API")
@RestController
@RequestMapping("/api/home")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    // TODO: JWT 연동 후 토큰에서 userId 추출로 교체
    private static final Long TEMP_USER_ID = 1L;

    @Operation(summary = "홈 통계 조회", description = "누적 참여자 수와 오늘 생성된 투표 수를 반환합니다.")
    @GetMapping("/stats")
    public ApiResponse<HomeStatsResponse> getStats() {
        return ApiResponse.ok(homeService.getStats());
    }

    @Operation(summary = "인기 포토스팟 조회", description = "전체 기간 swipe 추천율 기준 상위 3개 포토스팟을 반환합니다.")
    @GetMapping("/spots/popular")
    public ApiResponse<HomePopularSpotsResponse> getPopularSpots() {
        return ApiResponse.ok(homeService.getPopularSpots());
    }
}
