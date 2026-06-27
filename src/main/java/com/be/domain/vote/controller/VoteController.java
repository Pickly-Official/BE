package com.be.domain.vote.controller;

import com.be.domain.photo.dto.response.PhotoUploadResponse;
import com.be.domain.photo.service.PhotoService;
import com.be.domain.vote.dto.request.VoteCreateRequest;
import com.be.domain.vote.dto.response.VoteCreateResponse;
import com.be.domain.vote.service.VoteService;
import com.be.global.response.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;
    private final PhotoService photoService;

    @PostMapping
    public ResponseEntity<ApiResponse<VoteCreateResponse>> createVote(
            @RequestBody @Valid VoteCreateRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(voteService.createVote(request)));
    }

    @PostMapping(value = "/{voteId}/photos", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PhotoUploadResponse>> uploadPhotos(
            @PathVariable Long voteId,
            @RequestPart("files") List<MultipartFile> files
    ) {
        return ResponseEntity.ok(ApiResponse.ok(photoService.uploadPhotos(voteId, files)));
    }
}
