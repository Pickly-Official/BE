package com.be.domain.photo.controller;

import com.be.domain.photo.dto.response.PhotoLocationAnalysisResponse;
import com.be.domain.photo.service.PhotoService;
import com.be.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;

    @PostMapping(value = "/locations", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<PhotoLocationAnalysisResponse>> analyzeLocations(
            @RequestPart("files") List<MultipartFile> files
    ) {
        return ResponseEntity.ok(ApiResponse.ok(photoService.analyzeLocations(files)));
    }
}
