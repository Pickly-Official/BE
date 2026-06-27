package com.be.auth;

import com.be.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtProvider jwtProvider;

    public static final String USER_ID_ATTR = "userId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String method = request.getMethod();
        String path = request.getRequestURI();

        // GET /api/v1/votes/{id} 및 GET /api/v1/votes/{id}/results 는 공개
        if ("GET".equals(method) && path.matches("/api/v1/votes/\\d+(/results)?")) {
            return true;
        }
        // 스와이프/되돌리기는 X-Voter-Id 기반 (JWT 불필요)
        if (path.matches("/api/v1/votes/\\d+/swipe.*")) {
            return true;
        }

        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다");
        }
        Long userId = jwtProvider.parseUserId(header.substring(7));
        request.setAttribute(USER_ID_ATTR, userId);
        return true;
    }
}
