package com.be.domain.user.controller;

import com.be.domain.user.dto.response.UserProfileResponse;
import com.be.domain.user.dto.response.UserStatsResponse;
import com.be.domain.user.service.UserService;
import com.be.global.response.ApiResponse;
import com.be.global.security.SecurityUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<UserProfileResponse> getMyProfile() {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(userService.getProfile(userId));
    }

    @GetMapping("/me/stats")
    public ApiResponse<UserStatsResponse> getMyStats() {
        Long userId = SecurityUtil.getCurrentUserId();
        return ApiResponse.ok(userService.getStats(userId));
    }
}
