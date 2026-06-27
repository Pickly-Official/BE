# Swipe Vote Backend Design

**Date:** 2026-06-28  
**Scope:** 백엔드만 (Spring Boot + JPA)  
**Branch:** Feat/swipe

---

## 1. 목표

스와이프 투표 화면(`/vote/$id`)이 필요로 하는 두 개의 API 엔드포인트를 구현한다.

- `GET /api/votes/{voteId}` — 투표 진입 시 사진 목록 조회
- `POST /api/votes/{voteId}/results` — 4장 완료 후 like/skip 일괄 저장

---

## 2. 데이터 모델 변경

### 2.1 `Participant` 엔티티 수정

`deviceToken` 필드를 `voterUuid`로 rename한다. 현재 `ParticipantService` / `ParticipantController`가 stub(TODO)이므로 기존 비즈니스 로직 영향 없음.

| 변경 전 | 변경 후 |
|---------|---------|
| `String deviceToken` (col: `device_token`) | `String voterUuid` (col: `voter_uuid`) |

`userId` 컬럼을 추가한다 (nullable).

| 필드 | 타입 | 제약 |
|------|------|------|
| `userId` | `Long` | nullable — 로그인 사용자만 채워짐, 비회원은 null |

unique constraint: `(vote_id, voter_uuid)` — upsert 키.

### 2.2 `Swipe` / `SwipeChoice` — 변경 없음

`SwipeChoice.LIKE` / `SKIP` (대문자 enum) 그대로 유지.  
API 경계에서 소문자 `"like"` / `"skip"` 수신 시 `@JsonCreator`로 변환.

### 2.3 연관 DTO rename

- `ParticipantCreateRequest.deviceToken` → `voterUuid`
- `ParticipantResponse.deviceToken` → `voterUuid`

---

## 3. API 명세

### 3.1 `GET /api/votes/{voteId}`

투표 화면 진입 시 사진 목록 조회. 인증 불필요(공개).

**성공 응답 (200):**
```json
{
  "success": true,
  "code": "200",
  "message": "요청이 성공했습니다.",
  "data": {
    "voteId": 12,
    "title": "내 프로필 사진 골라줘",
    "photos": [
      { "photoId": 101, "imageUrl": "https://.../1.jpg", "order": 0 },
      { "photoId": 102, "imageUrl": "https://.../2.jpg", "order": 1 },
      { "photoId": 103, "imageUrl": "https://.../3.jpg", "order": 2 },
      { "photoId": 104, "imageUrl": "https://.../4.jpg", "order": 3 }
    ]
  }
}
```

**에러:**
- `VOTE_NOT_FOUND` (404) — 존재하지 않는 투표
- `VOTE_ALREADY_CLOSED` (400) — 마감된 투표

### 3.2 `POST /api/votes/{voteId}/results`

4장 결정을 일괄 제출. 로그인이면 `Authorization: Bearer <jwt>` 첨부, 비회원이면 헤더 없이 `voterUuid`만으로 식별.

**요청 body:**
```json
{
  "voterUuid": "f47ac10b-58cc-4372-a567-0e02b2c3d479",
  "results": [
    { "photoId": 101, "choice": "like" },
    { "photoId": 102, "choice": "skip" },
    { "photoId": 103, "choice": "like" },
    { "photoId": 104, "choice": "skip" }
  ]
}
```

**성공 응답 (200):**
```json
{
  "success": true,
  "code": "200",
  "message": "요청이 성공했습니다.",
  "data": { "submissionId": 55 }
}
```

**에러:**
- `VOTE_NOT_FOUND` (404)
- `VOTE_ALREADY_CLOSED` (400)
- `PHOTO_NOT_IN_VOTE` (400) — photoId가 해당 voteId 소속이 아님
- `INVALID_RESULT_COUNT` (400) — results 개수 ≠ 해당 vote의 사진 수

---

## 4. 서비스 / 리포지토리 레이어

### 4.1 Repository 추가 메서드

```java
// PhotoRepository
List<Photo> findByVoteIdOrderBySequenceAsc(Long voteId);

// ParticipantRepository
Optional<Participant> findByVoteIdAndVoterUuid(Long voteId, String voterUuid);

// SwipeRepository
void deleteAllByParticipant(Participant participant);
```

### 4.2 새 DTOs

```
participant/dto/request/SwipeSubmitRequest.java     — voterUuid + List<ResultItem>
participant/dto/request/SwipeSubmitRequest.ResultItem — photoId + choice(String)
participant/dto/response/SwipeSubmitResponse.java   — submissionId
vote/dto/response/VoteDetailResponse.java           — voteId + title + List<PhotoItem>
vote/dto/response/VoteDetailResponse.PhotoItem      — photoId + imageUrl + order
```

### 4.3 upsert 로직 (`ParticipantService.submitResults`)

1. Vote 조회 → `VOTE_NOT_FOUND` / `VOTE_ALREADY_CLOSED` 검증
2. 해당 vote의 photos 조회 → photoId 소속 검증, 개수 검증
3. `findByVoteIdAndVoterUuid` → 기존 Participant 있으면 기존 Swipe 전부 삭제 후 재사용 / 없으면 신규 생성
4. `SecurityContextHolder.getContext().getAuthentication()` — principal이 Long이면 userId, 없으면 null (기존 `JwtAuthenticationFilter`가 유효 JWT일 때만 SecurityContext 세팅)
5. choice `"like"`/`"skip"` → `SwipeChoice.LIKE`/`SKIP` 변환 후 Swipe 일괄 저장
6. `participant.complete()` 호출
7. `participantId`를 `submissionId`로 반환

### 4.4 Controller 배치

두 엔드포인트 모두 `VoteController` (`/api/votes`)에 추가.  
`ParticipantService.submitResults()`를 `VoteController`에서 호출.

---

## 5. 에러 코드 추가

`ErrorCode` enum에 두 항목 추가:

```java
PHOTO_NOT_IN_VOTE(HttpStatus.BAD_REQUEST, "PA003", "해당 투표의 사진이 아닙니다."),
INVALID_RESULT_COUNT(HttpStatus.BAD_REQUEST, "PA004", "결과 개수가 사진 수와 일치하지 않습니다."),
```

---

## 6. 구현 대상 파일 목록

| 액션 | 파일 |
|------|------|
| 수정 | `Participant.java` — voterUuid rename + userId 추가 |
| 수정 | `ParticipantCreateRequest.java` — deviceToken → voterUuid |
| 수정 | `ParticipantResponse.java` — deviceToken → voterUuid |
| 수정 | `SwipeChoice.java` — `@JsonCreator` 추가 |
| 수정 | `PhotoRepository.java` — findByVoteIdOrderBySequenceAsc |
| 수정 | `ParticipantRepository.java` — findByVoteIdAndVoterUuid |
| 수정 | `SwipeRepository.java` — deleteAllByParticipant |
| 수정 | `VoteService.java` — getVoteDetail 구현 (GET 응답 조립) |
| 수정 | `ParticipantService.java` — submitResults 구현 |
| 수정 | `VoteController.java` — GET /{voteId}, POST /{voteId}/results 추가 |
| 수정 | `ErrorCode.java` — PA003, PA004 추가 |
| 생성 | `SwipeSubmitRequest.java` |
| 생성 | `SwipeSubmitResponse.java` |
| 생성 | `VoteDetailResponse.java` |
