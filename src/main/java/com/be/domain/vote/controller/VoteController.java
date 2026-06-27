package com.be.domain.vote.controller;

import com.be.domain.participant.dto.request.SwipeSubmitRequest;
import com.be.domain.participant.dto.response.SwipeSubmitResponse;
import com.be.domain.participant.service.ParticipantService;
import com.be.domain.photo.dto.response.PhotoUploadResponse;
import com.be.domain.photo.service.PhotoService;
import com.be.domain.vote.dto.request.VoteCreateRequest;
import com.be.domain.vote.dto.response.VoteCreateResponse;
import com.be.domain.vote.dto.response.VoteDetailResponse;
import com.be.domain.vote.service.VoteService;
import com.be.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;
    private final PhotoService photoService;
    private final ParticipantService participantService;

    @PostMapping
    public ResponseEntity<ApiResponse<VoteCreateResponse>> createVote(
            @RequestBody @Valid VoteCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(voteService.createVote(request)));
    }

    @PostMapping(value = "/{voteId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PhotoUploadResponse>> uploadPhotos(
            @PathVariable Long voteId,
            @RequestPart("files") List<MultipartFile> files) {
        return ResponseEntity.ok(ApiResponse.ok(photoService.uploadPhotos(voteId, files)));
    }

    @GetMapping("/{voteId}")
    public ResponseEntity<ApiResponse<VoteDetailResponse>> getVoteDetail(
            @PathVariable Long voteId) {
        return ResponseEntity.ok(ApiResponse.ok(voteService.getVoteDetail(voteId)));
    }

    @PostMapping("/{voteId}/results")
    public ResponseEntity<ApiResponse<SwipeSubmitResponse>> submitResults(
            @PathVariable Long voteId,
            @RequestBody SwipeSubmitRequest request) {
        Long userId = extractUserId();
        return ResponseEntity.ok(ApiResponse.ok(participantService.submitResults(voteId, request, userId)));
    }

    private Long extractUserId() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth != null && auth.getPrincipal() instanceof Long userId) {
            return userId;
        }
        return null;
    }
}
