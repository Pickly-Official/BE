package com.be.domain.vote.service;

import com.be.domain.analysis.entity.PhotoAnalysis;
import com.be.domain.analysis.repository.PhotoAnalysisRepository;
import com.be.domain.participant.entity.SwipeChoice;
import com.be.domain.participant.repository.ParticipantRepository;
import com.be.domain.participant.repository.SwipeRepository;
import com.be.domain.photo.entity.Photo;
import com.be.domain.photo.repository.PhotoRepository;
import com.be.domain.user.entity.User;
import com.be.domain.user.repository.UserRepository;
import com.be.domain.vote.dto.request.VoteCreateRequest;
import com.be.domain.vote.dto.response.VoteCreateResponse;
import com.be.domain.vote.dto.response.VoteResultResponse;
import com.be.domain.vote.entity.Vote;
import com.be.domain.vote.repository.VoteRepository;
import com.be.global.exception.CustomException;
import com.be.global.exception.ErrorCode;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.client.JdkClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestClient;

import java.net.http.HttpClient;
import java.util.*;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VoteService {

    private final VoteRepository voteRepository;
    private final UserRepository userRepository;
    private final PhotoRepository photoRepository;
    private final ParticipantRepository participantRepository;
    private final SwipeRepository swipeRepository;
    private final PhotoAnalysisRepository photoAnalysisRepository;

    private final RestClient restClient = RestClient.builder()
            .requestFactory(new JdkClientHttpRequestFactory(
                    HttpClient.newBuilder()
                            .followRedirects(HttpClient.Redirect.ALWAYS)
                            .build()))
            .build();

    @Value("${gemini.api-key}")
    private String geminiApiKey;

    @Transactional
    public VoteCreateResponse createVote(VoteCreateRequest request) {
        // TODO: JWT 연동 후 SecurityContext에서 userId 추출로 교체
        User user = userRepository.findById(1L)
                .orElseThrow(() -> new CustomException(ErrorCode.USER_NOT_FOUND));

        Vote vote = Vote.of(user, request.title(), request.useLocation(), request.deadlineType());
        Vote saved = voteRepository.save(vote);

        return new VoteCreateResponse(saved.getId(), saved.getTitle(), saved.getClosedAt());
    }

    @Transactional
    public VoteResultResponse getVoteResult(Long voteId) {
        Vote vote = voteRepository.findById(voteId)
                .orElseThrow(() -> new CustomException(ErrorCode.VOTE_NOT_FOUND));

        List<Photo> photos = photoRepository.findByVoteId(voteId);
        int participantCount = (int) participantRepository.countByVoteId(voteId);

        List<PhotoRankData> rankData = photos.stream()
                .map(photo -> {
                    int likeCount = (int) swipeRepository.countByPhotoIdAndChoice(photo.getId(), SwipeChoice.LIKE);
                    int recommendRate = participantCount == 0 ? 0 : likeCount * 100 / participantCount;
                    return new PhotoRankData(photo, recommendRate);
                })
                .sorted(Comparator.comparingInt(PhotoRankData::recommendRate).reversed())
                .toList();

        List<VoteResultResponse.PhotoRankInfo> photoRankInfos = new ArrayList<>();
        for (int i = 0; i < rankData.size(); i++) {
            PhotoRankData data = rankData.get(i);
            int rank = i + 1;
            VoteResultResponse.PhotoAnalysisInfo analysisInfo = rank <= 3
                    ? getOrCreateAnalysis(data.photo(), data.recommendRate())
                    : null;

            photoRankInfos.add(new VoteResultResponse.PhotoRankInfo(
                    data.photo().getId(),
                    data.photo().getSequence(),
                    data.photo().getImageUrl(),
                    data.recommendRate(),
                    rank,
                    analysisInfo
            ));
        }

        String spotName = photos.stream()
                .filter(p -> p.getPhotoSpot() != null)
                .findFirst()
                .map(p -> p.getPhotoSpot().getName())
                .orElse(null);

        return new VoteResultResponse(voteId, vote.getTitle(), spotName, participantCount, photoRankInfos);
    }

    private VoteResultResponse.PhotoAnalysisInfo getOrCreateAnalysis(Photo photo, int recommendRate) {
        log.info("[Gemini] 분석 시작: photoId={}, recommendRate={}", photo.getId(), recommendRate);

        Optional<PhotoAnalysis> existing = photoAnalysisRepository.findByPhoto(photo);
        if (existing.isPresent()) {
            log.info("[Gemini] 기존 분석 재사용: photoId={}", photo.getId());
            return toAnalysisInfo(existing.get());
        }

        try {
            byte[] imageBytes = fetchImageBytes(photo.getImageUrl());
            if (imageBytes == null) return null;

            log.info("[Gemini] 이미지 fetch 성공: photoId={}, size={}bytes", photo.getId(), imageBytes.length);

            String base64 = Base64.getEncoder().encodeToString(imageBytes);
            String responseText = callGemini(base64, buildPrompt(recommendRate));
            if (responseText == null) return null;

            log.info("[Gemini] 응답 원문: {}", responseText);

            String json = responseText.trim()
                    .replaceAll("(?s)^```json\\s*", "")
                    .replaceAll("(?s)\\s*```$", "")
                    .trim();
            JsonObject parsed = JsonParser.parseString(json).getAsJsonObject();
            String type = parsed.get("type").getAsString();

            PhotoAnalysis analysis;
            if ("person".equals(type)) {
                analysis = PhotoAnalysis.ofPerson(photo,
                        getStringOrNull(parsed, "composition"),
                        getStringOrNull(parsed, "expression"),
                        getStringOrNull(parsed, "lighting"));
            } else {
                analysis = PhotoAnalysis.ofScene(photo,
                        getStringOrNull(parsed, "mood"),
                        getStringOrNull(parsed, "lighting"));
            }

            photoAnalysisRepository.save(analysis);
            log.info("[Gemini] 분석 저장 완료: photoId={}, type={}", photo.getId(), type);
            return toAnalysisInfo(analysis);
        } catch (Exception e) {
            log.error("[Gemini] 분석 실패: photoId={}, {}", photo.getId(), e.getMessage(), e);
            return null;
        }
    }

    private byte[] fetchImageBytes(String url) {
        log.info("[Gemini] fetch 시작: {}", url);
        try {
            log.info("[Gemini] HTTP 요청 전");
            byte[] bytes = restClient.get().uri(url).retrieve().body(byte[].class);
            log.info("[Gemini] HTTP 요청 완료: {}bytes", bytes != null ? bytes.length : "null");
            return bytes;
        } catch (Exception e) {
            log.warn("[Gemini] fetch Exception: {} - {}", e.getClass().getSimpleName(), e.getMessage());
            return null;
        } catch (Throwable t) {
            log.error("[Gemini] fetch Throwable: {} - {}", t.getClass().getSimpleName(), t.getMessage());
            return null;
        }
    }

    private String callGemini(String base64Image, String prompt) {
        try {
            Map<String, Object> request = Map.of(
                    "contents", List.of(Map.of(
                            "parts", List.of(
                                    Map.of("inlineData", Map.of("mimeType", "image/jpeg", "data", base64Image)),
                                    Map.of("text", prompt)
                            )
                    ))
            );

            GeminiResponse response = restClient.post()
                    .uri("https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent?key={key}",
                            geminiApiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(request)
                    .retrieve()
                    .body(GeminiResponse.class);

            if (response == null || response.candidates == null || response.candidates.isEmpty()) {
                log.warn("[Gemini] 응답 비어있음");
                return null;
            }
            return response.candidates.get(0).content.parts.get(0).text;
        } catch (Exception e) {
            log.error("[Gemini] API 호출 실패: {}", e.getMessage(), e);
            return null;
        }
    }

    private String buildPrompt(int recommendRate) {
        return """
                이 사진은 친구들의 스와이프 투표에서 상위 3위 안에 든 사진입니다.
                추천율: %d%%

                사진을 분석해서 아래 JSON 형식으로만 응답하세요. 다른 텍스트는 절대 포함하지 마세요.

                사람이 있는 사진이면:
                {"type": "person", "composition": "구도 한 문장", "expression": "표정 한 문장", "lighting": "조명 한 문장"}

                사람이 없는 사진이면:
                {"type": "scene", "mood": "전체 분위기 한 문장", "lighting": "조명 한 문장"}

                각 항목은 20자 이내로 짧고 간결하게.
                """.formatted(recommendRate);
    }

    private VoteResultResponse.PhotoAnalysisInfo toAnalysisInfo(PhotoAnalysis analysis) {
        if ("scene".equals(analysis.getType())) {
            return new VoteResultResponse.PhotoAnalysisInfo(
                    "scene", null, null, analysis.getLighting(), analysis.getComposition());
        }
        return new VoteResultResponse.PhotoAnalysisInfo(
                "person", analysis.getComposition(), analysis.getExpression(), analysis.getLighting(), null);
    }

    private String getStringOrNull(JsonObject obj, String key) {
        return obj.has(key) && !obj.get(key).isJsonNull() ? obj.get(key).getAsString() : null;
    }

    private record PhotoRankData(Photo photo, int recommendRate) {}

    private static class GeminiResponse {
        public List<Candidate> candidates;
    }

    private static class Candidate {
        public Content content;
    }

    private static class Content {
        public List<Part> parts;
    }

    private static class Part {
        public String text;
    }
}
