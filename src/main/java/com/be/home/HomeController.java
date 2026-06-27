package com.be.home;

import com.be.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HomeController {

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.ok("ok");
    }

    @GetMapping("/home/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.ok(Map.of("totalVoters", 0, "todayVotes", 0));
    }

    @GetMapping("/home/hot-spots")
    public ApiResponse<List<Object>> hotSpots() {
        return ApiResponse.ok(List.of());
    }
}
