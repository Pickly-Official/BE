package com.be.user;

import com.be.auth.AuthInterceptor;
import com.be.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> getMe(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.USER_ID_ATTR);
        User user = userService.findById(userId);
        return ApiResponse.ok(Map.of(
                "id", user.getId(),
                "nickname", user.getNickname(),
                "provider", user.getProvider(),
                "stats", Map.of("createdVotes", 0, "receivedVotes", 0, "bestCuts", 0)
        ));
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMe(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.USER_ID_ATTR);
        userService.deleteById(userId);
    }
}
