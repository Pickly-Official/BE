package com.be.domain.photo.controller;

import com.be.domain.photo.dto.response.PhotoLocationAnalysisResponse;
import com.be.domain.photo.service.PhotoService;
import com.be.global.response.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/photos")
@RequiredArgsConstructor
public class PhotoController {

    private final PhotoService photoService;

    @PostMapping("/locations")
    public ResponseEntity<ApiResponse<PhotoLocationAnalysisResponse>> analyzeLocations(
            @RequestParam(value = "files", required = false) List<MultipartFile> files
    ) {
        return ResponseEntity.ok(ApiResponse.ok(photoService.analyzeLocations(files)));
    }
}
