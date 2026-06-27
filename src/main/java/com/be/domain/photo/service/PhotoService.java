package com.be.domain.photo.service;

import com.be.domain.photo.dto.response.PhotoUploadResponse;
import com.be.domain.vote.dto.response.VotePhotosResponse;
import com.be.domain.photo.entity.Photo;
import com.be.domain.photo.entity.PhotoSpot;
import com.be.domain.photo.repository.PhotoRepository;
import com.be.domain.vote.entity.Vote;
import com.be.domain.vote.repository.VoteRepository;
import com.be.global.exception.CustomException;
import com.be.global.exception.ErrorCode;
import com.drew.imaging.ImageMetadataReader;
import com.drew.lang.GeoLocation;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.drew.metadata.exif.GpsDirectory;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class PhotoService {

    private final PhotoRepository photoRepository;
    private final VoteRepository voteRepository;
    private final PhotoSpotService photoSpotService;
    private final S3Client s3Client;

    private final RestClient restClient = RestClient.create();

    @Value("${kakao.rest-api-key}")
    private String kakaoApiKey;

    @Value("${aws.s3.bucket}")
    private String s3Bucket;

    @Value("${aws.s3.region}")
    private String s3Region;

    @Transactional
    public PhotoUploadResponse uploadPhotos(Long voteId, List<MultipartFile> files) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));

        if (files.size() < 2 || files.size() > 10) {
            throw new CustomException(ErrorCode.INVALID_PHOTO_COUNT);
        }

        for (MultipartFile file : files) {
            if (file.isEmpty()) throw new CustomException(ErrorCode.INVALID_PHOTO_COUNT);
            String contentType = file.getContentType();
            if (contentType == null || !contentType.startsWith("image/")) {
                throw new CustomException(ErrorCode.INVALID_FILE_TYPE);
            }
        }

        List<Photo> photos = new ArrayList<>();
        for (int i = 0; i < files.size(); i++) {
            int sequence = i + 1;
            MultipartFile file = files.get(i);
            GpsData gps = parseGpsFromExif(file);

            if (vote.getUseLocation() && gps == null) {
                throw new CustomException(ErrorCode.NO_EXIF_GPS);
            }

            String imageUrl = uploadToS3(file, voteId, sequence, UUID.randomUUID().toString());

            Photo photo = (gps != null)
                    ? Photo.ofWithLocation(vote, imageUrl, sequence,
                            BigDecimal.valueOf(gps.latitude()), BigDecimal.valueOf(gps.longitude()), gps.takenAt())
                    : Photo.of(vote, imageUrl, sequence);
            photos.add(photo);
        }

        Map<Photo, String> spotNameByPhoto = new HashMap<>();
        for (List<Photo> group : groupByHaversine(photos)) {
            Photo first = group.get(0);
            double centerLat = first.getLatitude().doubleValue();
            double centerLng = first.getLongitude().doubleValue();

            String spotName = getSpotNameFromKakao(centerLng, centerLat);
            if (spotName == null) continue;

            PhotoSpot spot = photoSpotService.findOrCreate(
                    spotName, BigDecimal.valueOf(centerLat), BigDecimal.valueOf(centerLng));
            for (Photo p : group) {
                spot.increasePhotoCount();
                p.assignSpot(spot);
                spotNameByPhoto.put(p, spotName);
            }
        }

        List<Photo> saved = photoRepository.saveAll(photos);
        List<PhotoUploadResponse.PhotoInfo> photoInfos = saved.stream()
                .map(p -> new PhotoUploadResponse.PhotoInfo(
                        p.getId(), p.getSequence(), p.getImageUrl(), spotNameByPhoto.get(p)))
                .toList();

        return new PhotoUploadResponse(photoInfos);
    }

    public VotePhotosResponse getVotePhotos(Long voteId) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));

        List<VotePhotosResponse.PhotoInfo> photoInfos = photoRepository
                .findByVoteIdOrderBySequenceAsc(voteId).stream()
                .map(p -> new VotePhotosResponse.PhotoInfo(
                        p.getId(), p.getImageUrl(), p.getSequence()))
                .toList();

        return new VotePhotosResponse(vote.getId(), vote.getTitle(), photoInfos);
    }

    private String uploadToS3(MultipartFile file, Long voteId, int sequence, String uuid) {
        String key = "votes/%d/%d_%s%s".formatted(voteId, sequence, uuid, resolveExtension(file));
        try {
            PutObjectRequest request = PutObjectRequest.builder()
                    .bucket(s3Bucket)
                    .key(key)
                    .contentType(file.getContentType())
                    .contentLength(file.getSize())
                    .build();

            s3Client.putObject(request, RequestBody.fromInputStream(file.getInputStream(), file.getSize()));
            return "https://%s.s3.%s.amazonaws.com/%s".formatted(s3Bucket, s3Region, key);
        } catch (IOException | RuntimeException e) {
            log.error("[S3] 업로드 실패: voteId={}, sequence={}, filename={}",
                    voteId, sequence, file.getOriginalFilename(), e);
            throw new CustomException(ErrorCode.S3_UPLOAD_FAILED);
        }
    }

    private String resolveExtension(MultipartFile file) {
        String filename = file.getOriginalFilename();
        if (filename != null) {
            int dotIndex = filename.lastIndexOf('.');
            if (dotIndex >= 0 && dotIndex < filename.length() - 1) {
                String extension = filename.substring(dotIndex).toLowerCase(Locale.ROOT);
                if (extension.matches("\\.[a-z0-9]{1,10}")) {
                    return extension;
                }
            }
        }

        String contentType = file.getContentType();
        if ("image/png".equals(contentType)) return ".png";
        if ("image/webp".equals(contentType)) return ".webp";
        if ("image/gif".equals(contentType)) return ".gif";
        return ".jpg";
    }

    private GpsData parseGpsFromExif(MultipartFile file) {
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file.getInputStream());
            GpsDirectory gpsDir = metadata.getFirstDirectoryOfType(GpsDirectory.class);
            if (gpsDir == null) {
                log.warn("[EXIF] GPS 디렉토리 없음: {}", file.getOriginalFilename());
                return null;
            }

            GeoLocation location = gpsDir.getGeoLocation();
            if (location == null || location.isZero()) {
                log.warn("[EXIF] GPS 좌표 없음 또는 0,0: {}", file.getOriginalFilename());
                return null;
            }

            log.info("[EXIF] GPS 파싱 성공: lat={}, lng={}", location.getLatitude(), location.getLongitude());

            LocalDateTime takenAt = null;
            ExifSubIFDDirectory exifDir = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (exifDir != null) {
                Date date = exifDir.getDate(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (date != null) {
                    takenAt = date.toInstant().atZone(ZoneId.of("Asia/Seoul")).toLocalDateTime();
                }
            }
            return new GpsData(location.getLatitude(), location.getLongitude(), takenAt);
        } catch (Exception e) {
            log.error("[EXIF] 파싱 실패: {} - {}", file.getOriginalFilename(), e.getMessage());
            return null;
        }
    }

    private List<List<Photo>> groupByHaversine(List<Photo> photos) {
        List<Photo> withGps = photos.stream()
                .filter(p -> p.getLatitude() != null).toList();

        List<List<Photo>> groups = new ArrayList<>();
        for (Photo photo : withGps) {
            boolean added = false;
            for (List<Photo> group : groups) {
                Photo rep = group.get(0);
                if (haversineDistance(
                        rep.getLatitude().doubleValue(), rep.getLongitude().doubleValue(),
                        photo.getLatitude().doubleValue(), photo.getLongitude().doubleValue()) <= 50.0) {
                    group.add(photo);
                    added = true;
                    break;
                }
            }
            if (!added) {
                List<Photo> g = new ArrayList<>();
                g.add(photo);
                groups.add(g);
            }
        }
        return groups;
    }

    private double haversineDistance(double lat1, double lon1, double lat2, double lon2) {
        final double R = 6371000.0;
        double dLat = Math.toRadians(lat2 - lat1);
        double dLon = Math.toRadians(lon2 - lon1);
        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                * Math.sin(dLon / 2) * Math.sin(dLon / 2);
        return R * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private String getSpotNameFromKakao(double longitude, double latitude) {
        // 1. 좌표 → 주소
        String address = resolveAddress(longitude, latitude);
        if (address == null) return null;

        // 2. 주소로 키워드 검색 → place_name
        String placeName = searchByKeyword(address, longitude, latitude);
        if (placeName != null) return placeName;

        // 3. 장소명 못 찾으면 동 이름 폴백
        return fallbackToDong(longitude, latitude);
    }

    private String resolveAddress(double longitude, double latitude) {
        try {
            KakaoAddressResponse response = restClient.get()
                    .uri("https://dapi.kakao.com/v2/local/geo/coord2address.json?x={lng}&y={lat}",
                            longitude, latitude)
                    .header("Authorization", "KakaoAK " + kakaoApiKey)
                    .retrieve()
                    .body(KakaoAddressResponse.class);

            if (response == null || response.documents == null || response.documents.isEmpty()) return null;
            KakaoDocument doc = response.documents.get(0);
            if (doc.roadAddress != null && doc.roadAddress.addressName != null) {
                return doc.roadAddress.addressName;
            }
            if (doc.address != null) return doc.address.addressName;
            return null;
        } catch (Exception e) {
            log.error("[Kakao] 주소 조회 실패: {}", e.getMessage());
            return null;
        }
    }

    private String searchByKeyword(String query, double longitude, double latitude) {
        try {
            KakaoKeywordResponse response = restClient.get()
                    .uri("https://dapi.kakao.com/v2/local/search/keyword.json?query={q}&x={lng}&y={lat}&radius=50&sort=distance",
                            query, longitude, latitude)
                    .header("Authorization", "KakaoAK " + kakaoApiKey)
                    .retrieve()
                    .body(KakaoKeywordResponse.class);

            if (response == null || response.documents == null || response.documents.isEmpty()) return null;
            String placeName = response.documents.get(0).placeName;
            log.info("[Kakao] 장소명 추출 성공: {}", placeName);
            return placeName;
        } catch (Exception e) {
            log.warn("[Kakao] 키워드 검색 실패: {}", e.getMessage());
            return null;
        }
    }

    private String fallbackToDong(double longitude, double latitude) {
        try {
            KakaoAddressResponse response = restClient.get()
                    .uri("https://dapi.kakao.com/v2/local/geo/coord2address.json?x={lng}&y={lat}",
                            longitude, latitude)
                    .header("Authorization", "KakaoAK " + kakaoApiKey)
                    .retrieve()
                    .body(KakaoAddressResponse.class);

            if (response == null || response.documents == null || response.documents.isEmpty()) return null;
            KakaoDocument doc = response.documents.get(0);
            if (doc.address == null) return null;
            log.info("[Kakao] 동 이름 폴백: {}", doc.address.region3depthName);
            return doc.address.region3depthName;
        } catch (Exception e) {
            log.error("[Kakao] 동 이름 폴백 실패: {}", e.getMessage());
            return null;
        }
    }

    private record GpsData(double latitude, double longitude, LocalDateTime takenAt) {}

    private static class KakaoAddressResponse {
        public List<KakaoDocument> documents;
    }

    private static class KakaoDocument {
        public KakaoAddress address;
        @JsonProperty("road_address")
        public KakaoRoadAddress roadAddress;
    }

    private static class KakaoAddress {
        @JsonProperty("address_name")
        public String addressName;
        @JsonProperty("region_3depth_name")
        public String region3depthName;
    }

    private static class KakaoRoadAddress {
        @JsonProperty("address_name")
        public String addressName;
    }

    private static class KakaoKeywordResponse {
        public List<KakaoKeywordDocument> documents;
    }

    private static class KakaoKeywordDocument {
        @JsonProperty("place_name")
        public String placeName;
    }
}
