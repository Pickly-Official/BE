package com.be.auth;

import com.be.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login/{provider}")
    public ApiResponse<Map<String, Object>> login(
            @PathVariable String provider,
            @RequestBody Map<String, String> body) {
        String accessToken = body.get("accessToken");
        return ApiResponse.ok(authService.login(provider, accessToken));
    }
}
