package com.be.domain.mypage.controller;

import com.be.domain.mypage.dto.response.MypageStatsResponse;
import com.be.domain.mypage.service.MypageService;
import com.be.global.response.ApiResponse;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "Mypage", description = "마이페이지 API")
@RestController
@RequestMapping("/api/mypage")
@RequiredArgsConstructor
public class MypageController {

    private final MypageService mypageService;

    @Operation(summary = "마이페이지 통계 조회", description = "내가 만든 투표 수와 받은 스와이프 수를 반환합니다.")
    @GetMapping("/stats")
    public ApiResponse<MypageStatsResponse> getStats(@AuthenticationPrincipal Long userId) {
        return ApiResponse.ok(mypageService.getStats(userId));
    }
}
