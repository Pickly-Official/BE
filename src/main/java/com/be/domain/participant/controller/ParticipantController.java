package com.be.domain.participant.controller;

import com.be.domain.participant.dto.request.SwipeRequest;
import com.be.domain.participant.dto.response.SwipeResponse;
import com.be.domain.participant.service.ParticipantService;
import com.be.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/participants")
@RequiredArgsConstructor
public class ParticipantController {

    private final ParticipantService participantService;

    @PostMapping("/{participantId}/swipes")
    public ResponseEntity<ApiResponse<SwipeResponse>> saveSwipe(
            @PathVariable Long participantId,
            @RequestBody SwipeRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(participantService.saveSwipe(participantId, request)));
    }
}
