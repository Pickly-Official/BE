package com.be.domain.vote.controller;

import com.be.domain.participant.dto.request.ParticipantCreateRequest;
import com.be.domain.participant.dto.response.ParticipantResponse;
import com.be.domain.participant.service.ParticipantService;
import com.be.domain.photo.dto.response.PhotoUploadResponse;
import com.be.domain.photo.service.PhotoService;
import com.be.domain.vote.dto.request.VoteCreateRequest;
import com.be.domain.vote.dto.response.MyVoteListResponse;
import com.be.domain.vote.dto.response.VoteAnalyzeResponse;
import com.be.domain.vote.dto.response.VoteCreateResponse;
import com.be.domain.vote.dto.response.VotePhotosResponse;
import com.be.domain.vote.dto.response.VoteResultResponse;
import com.be.domain.vote.service.VoteService;
import com.be.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
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
            @AuthenticationPrincipal Long userId,
            @RequestBody @Valid VoteCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(voteService.createVote(userId, request)));
    }

    @GetMapping("/my")
    public ResponseEntity<ApiResponse<MyVoteListResponse>> getMyVotes(
            @AuthenticationPrincipal Long userId) {
        return ResponseEntity.ok(ApiResponse.ok(voteService.getMyVotes(userId)));
    }

    @PostMapping(value = "/{voteId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PhotoUploadResponse>> uploadPhotos(
            @PathVariable Long voteId,
            @RequestPart("files") List<MultipartFile> files
    ) {
        return ResponseEntity.ok(ApiResponse.ok(photoService.uploadPhotos(voteId, files)));
    }

    @PostMapping("/{voteId}/participants")
    public ResponseEntity<ApiResponse<ParticipantResponse>> registerParticipant(
            @PathVariable Long voteId,
            @RequestBody ParticipantCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(participantService.register(voteId, request)));
    }

    @GetMapping("/{voteId}/photos")
    public ResponseEntity<ApiResponse<VotePhotosResponse>> getVotePhotos(@PathVariable Long voteId) {
        return ResponseEntity.ok(ApiResponse.ok(photoService.getVotePhotos(voteId)));
    }

    @GetMapping("/{voteId}/result")
    public ResponseEntity<ApiResponse<VoteResultResponse>> getVoteResult(@PathVariable Long voteId) {
        return ResponseEntity.ok(ApiResponse.ok(voteService.getVoteResult(voteId)));
    }

    @GetMapping("/{voteId}/analyze")
    public ResponseEntity<ApiResponse<VoteAnalyzeResponse>> analyzeVote(@PathVariable Long voteId) {
        return ResponseEntity.ok(ApiResponse.ok(voteService.analyzeVote(voteId)));
    }
}
