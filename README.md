# 📸 Pickly: 친구들의 스와이프와 AI 분석으로 베스트컷을 찾아주는 사진 투표 서비스
<img width="1280" height="832" alt="1" src="https://github.com/user-attachments/assets/e0f9ec0b-b492-4f28-becb-366a89a6b276" />

# 🌍 프로젝트 소개
### "친구들의 반응으로 베스트컷을 찾아보세요."

> 사진을 업로드하고 링크를 공유하면,
친구들의 스와이프 결과와 AI 분석을 기반으로
가장 좋은 사진을 객관적으로 추천해주는 서비스입니다.

### ❓ Why Pickly?: 기존 방식의 한계
| 비교 항목          | 💬 단톡방 | 📱 인스타 스토리 투표 | 📋 구글폼 | 🚀 **Pickly** |
| -------------- | :----: | :-----------: | :----: | :-----------: |
| **사진 여러 장 비교** |    △   |  ✕ (2지선다 위주)  |    △   |       ✅       |
| **결과 자동 집계**   |    ✕   |       △       |    ✅   |       ✅       |
| **AI 분석 제공**   |    ✕   |       ✕       |    ✕   |       ✅       |
| **참여 편의성**     |    △   |       ✅       |    ✕   |    ✅ (스와이프)   |
| **공간 데이터 활용**  |    ✕   |       ✕       |    ✕   |       ✅       |

### 🎯 Target Users
<img width="680" height="532" alt="4" src="https://github.com/user-attachments/assets/fa4b9ca1-8828-419c-9b5c-a5d9effa09bd" />

### 🕸️ Mesh 해커톤 주제: 연결(Connect)
> 사람·정보·공간을 잇는 서비스
<img width="680" height="532" alt="8" src="https://github.com/user-attachments/assets/4d89a3da-2086-4c82-b774-e16dcc736f66" />

# 👨‍👩‍👧‍👦 Team
<table align="center">
<tr>
<td align="center" width="170">
<b>PM · Design</b><br>
<img src="https://github.com/tellgeniewish.png" width="70"/><br>
<b>지니</b><br>
<sub>김현진</sub><br>
<a href="https://github.com/tellgeniewish">@tellgeniewish</a>
</td>
</tr>
</table>

<table align="center">
<tr>
<td align="center" width="170">
<b>Backend Lead</b><br>
<img src="https://github.com/sjinssun.png" width="70"/><br>
<b>제이스</b><br>
<sub>인석진</sub><br>
<a href="https://github.com/sjinssun">@sjinssun</a>
</td>

<td align="center" width="170">
<b>Backend</b><br>
<img src="https://github.com/Dianaland1112.png" width="70"/><br>
<b>딘</b><br>
<sub>정다연</sub><br>
<a href="https://github.com/Dianaland1112">@Dianaland1112</a>
</td>

<td align="center" width="170">
<b>Backend</b><br>
<img src="https://github.com/jinha1665.png" width="70"/><br>
<b>지나</b><br>
<sub>천진하</sub><br>
<a href="https://github.com/jinha1665">@jinha1665</a>
</td>
</tr>
</table>

<table align="center">
<tr>
<td align="center" width="170">
<b>Frontend Lead</b><br>
<img src="https://github.com/zio0225.png" width="70"/><br>
<b>지오</b><br>
<sub>김지오</sub><br>
<a href="https://github.com/zio0225">@zio0225</a>
</td>

<td align="center" width="170">
<b>Frontend</b><br>
<img src="https://github.com/gangdogang.png" width="70"/><br>
<b>동구</b><br>
<sub>원도경</sub><br>
<a href="https://github.com/gangdogang">@gangdogang</a>
</td>

<td align="center" width="170">
<b>Frontend</b><br>
<img src="https://github.com/SooyeonMoon.png" width="70"/><br>
<b>줄리</b><br>
<sub>문수연</sub><br>
<a href="https://github.com/SooyeonMoon">@SooyeonMoon</a>
</td>
</tr>
</table>

# 🚀 배포 주소
🌐 https://unions-coleman-focusing-firefox.trycloudflare.com/

# 💻 Tech Stack
<img width="586" height="352" alt="시스템 아키텍처" src="https://github.com/user-attachments/assets/2aad2913-c1ce-427c-b6c5-bca83ece4151" />

| 분류 | 기술 |
|------|------|
| Language | Java 21 |
| Framework | Spring Boot |
| Security | Spring Security, JWT |
| ORM | Spring Data JPA (Hibernate) |
| Database | MySQL |
| Build Tool | Gradle |
| Documentation | Swagger (SpringDoc OpenAPI) |
| AI | gemini-2.5-flash |
| Utility | Lombok |

<img width="424" height="480" alt="image" src="https://github.com/user-attachments/assets/c4876e22-d02c-4c53-bb79-667afdd68dcb" />

# ✨ Main Features

## 1. 소셜 로그인

- Google OAuth 로그인
- JWT 기반 인증
- 사용자 정보 저장

## 2. 투표 생성

- 사진 2~10장 업로드
- S3 저장
- 위치 정보(EXIF) 활용 여부 선택
- 투표 마감 기간 설정
- 공유 링크 생성

## 3. 친구 투표

- 로그인 없이 참여
- 좌우 스와이프 평가
- 중복 참여 방지
- 실시간 투표 집계

## 4. AI 분석

- 상위 사진 분석
- 베스트컷 선정 이유 제공
- 사진 분위기 분석
- 공간 키워드 생성

## 5. 결과 조회

- 추천율 순위
- AI 분석 결과
- 참여자 수
- 결과 이미지 저장

# 📂 Project Structure

```
src/main/java/com/be
├── domain
│   ├── analysis
│   ├── home
│   ├── mypage
│   ├── participant
│   ├── photo
│   ├── user
│   └── vote
│
└── global
    ├── common
    ├── config
    ├── exception
    ├── response
    └── security
```

# 🔄 User Flow
<img width="680" height="532" alt="5" src="https://github.com/user-attachments/assets/426bf3a2-894a-442e-a8b8-11c00796e027" />

```
Google Login
    │
    ▼
Home ──▶ Create Vote ──▶ Upload Images ──▶ Generate Link ──▶ Share Link
                                                              │
                                                              ▼
                                                    Friend Swipe
                                                              │
                                                              ▼
                                                     Vote Finished
                                                              │
                                                              ▼
                                                      AI Analysis
                                                              │
                                                              ▼
                                                          Result
```

# 🔥 AI Pipeline

```
📸 Upload
    ↓
🗳️ Vote
    ↓
🏆 Top 3
    ↓
🤖 Gemini 2.5 Flash
    ↓
✨ AI Analysis
    • Best-cut Reason
    • Mood Analysis
    • Location Keywords
    ↓
📊 Result
```

# 📌 Convention
## 📝 Commit
```
[type] #IssueNumber 제목

ex) [Feat] #11 로그인 서버 연동
```

### Type

| Type | Description |
|------|-------------|
| Feat | 새로운 기능 구현 |
| Mod | 코드 및 내부 파일 수정 |
| Add | 라이브러리 및 파일 추가 |
| Delete | 코드 및 파일 삭제 |
| Fix | 버그 수정 |
| Chore | 설정 변경 |
| HOTFIX | 긴급 수정 |
| Rename | 파일명 변경 |
| Docs | 문서 작성 |
| Refactor | 리팩터링 |
| Comment | 주석 추가 |

## 📝 Pull Request

```
[type] 이슈 제목

ex) [Feat] 로그인 기능 구현
```

## 📝 Branch

```
main
 ├── develop
 │
 ├── feat/#1-login
 ├── feat/#2-vote
 ├── feat/#3-ai
 ├── feat/#4-result
 └── fix/#5-login
```

# 💡 Future Work

- Kakao Map API 기반 위치 검색
- EXIF 자동 위치 그룹화
- AI 업스케일링
- 예상 추천율 AI
- 인기 포토스팟 랭킹
- 실시간 투표 현황
