package com.be.domain.vote.controller;

import com.be.domain.vote.dto.request.VoteCreateRequest;
import com.be.domain.vote.dto.response.VoteCreateResponse;
import com.be.domain.vote.service.VoteService;
import com.be.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;

    @PostMapping
    public ResponseEntity<ApiResponse<VoteCreateResponse>> createVote(
            @RequestBody @Valid VoteCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(voteService.createVote(request)));
    }
}
