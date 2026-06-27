package com.be.global.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;
import java.util.Map;

@RestController
public class HealthController {

    @GetMapping({"/health", "/api/health"})
    public Map<String, String> health() {
        return Map.of(
                "status", "ok",
                "timestamp", Instant.now().toString()
        );
    }
}
