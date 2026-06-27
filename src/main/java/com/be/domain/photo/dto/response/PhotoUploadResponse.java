package com.be.domain.photo.dto.response;

import java.util.List;

public record PhotoUploadResponse(List<PhotoInfo> photos) {
    public record PhotoInfo(Long photoId, Integer sequence, String imageUrl, String spotName) {}
}
