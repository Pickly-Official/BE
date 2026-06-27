# Pickly BE AWS EC2 Deployment

이 프로젝트는 Spring Boot + RDS MySQL 조합이며, EC2에서는 Docker Compose로 앱 컨테이너만 올리는 구성이 가장 단순합니다.

## 현재 권장 대상

- Region: `ap-northeast-2`
- EC2: `pickly-server`
- Public IP: `15.164.97.188`
- Open port: `8080`

`pickly-server-p2`는 현재 `default` 보안 그룹을 사용하므로 API 서버로 쓰려면 보안 그룹을 먼저 바꿔야 합니다.

## 1. EC2 접속 후 Docker 설치

Ubuntu EC2에서 실행합니다.

```bash
sudo apt update
sudo apt install -y git docker.io docker-compose-v2
sudo systemctl enable --now docker
sudo usermod -aG docker ubuntu
newgrp docker
```

설치 확인:

```bash
docker --version
docker compose version
```

## 2. 백엔드 코드 받기

```bash
git clone https://github.com/Pickly-Official/BE.git pickly-be
cd pickly-be
```

이미 clone되어 있으면:

```bash
cd pickly-be
git pull
```

## 3. 환경변수 설정

```bash
cp .env.example .env
nano .env
```

최소 예시:

```env
DB_NAME=pickly
DB_URL=jdbc:mysql://<rds-endpoint>:3306/pickly?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
DB_USERNAME=admin
DB_PASSWORD=<rds-password>
DB_DRIVER_CLASS_NAME=com.mysql.cj.jdbc.Driver
HIBERNATE_DIALECT=org.hibernate.dialect.MySQLDialect
JWT_SECRET=change-this-to-a-random-secret-at-least-32-characters
CORS_ALLOWED_ORIGINS=https://pickly-eta.vercel.app,http://localhost:3000,http://localhost:5173,http://127.0.0.1:5173
OAUTH2_REDIRECT_URI=https://pickly-eta.vercel.app/oauth2/callback
GOOGLE_CLIENT_ID=<google-client-id>
GOOGLE_CLIENT_SECRET=<google-client-secret>
S3_BUCKET=pickly-images
AWS_REGION=ap-northeast-2
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
KAKAO_REST_API_KEY=<kakao-rest-api-key>
GEMINI_API_KEY=<gemini-api-key>
OPENAI_API_KEY=
SPRING_JPA_HIBERNATE_DDL_AUTO=update
```

초기 배포는 `SPRING_JPA_HIBERNATE_DDL_AUTO=update`로 테이블을 자동 생성합니다. 운영 안정화 후에는 migration 도구를 붙이고 `none`으로 되돌리는 편이 좋습니다.

## 4. 서버 실행

```bash
docker compose -f docker-compose.prod.yml up -d --build
```

상태 확인:

```bash
docker compose -f docker-compose.prod.yml ps
docker compose -f docker-compose.prod.yml logs -f app
```

헬스체크:

```bash
curl http://localhost:8080/api/health
curl http://15.164.97.188:8080/api/health
```

정상 응답:

```json
{"timestamp":"...","status":"ok"}
```

## 5. 프론트 연결

Vercel 환경변수에 아래 값을 설정합니다.

```env
VITE_API_BASE_URL=http://15.164.97.188:8080/api
```

단, 프론트가 HTTPS인 상태에서 HTTP API를 호출하면 브라우저가 막을 수 있습니다. 실제 운영 연결은 아래 HTTPS 구성까지 끝낸 뒤 사용하는 것이 맞습니다.

## 6. HTTPS 운영 구성

운영 권장 구성:

1. Route 53 또는 사용 중인 DNS에서 `api` 서브도메인을 EC2 또는 ALB로 연결
2. Nginx 또는 ALB를 앞단에 배치
3. ACM 또는 Let's Encrypt로 TLS 인증서 적용
4. 프론트 Vercel 환경변수 변경

```env
VITE_API_BASE_URL=https://api.your-domain.com/api
```

## 7. 현재 가장 흔한 장애 원인

- `curl http://15.164.97.188:8080/api/health`가 connection refused:
  - 앱 컨테이너가 안 떠 있음
  - `docker compose logs app` 확인
- timeout:
  - 보안 그룹 인바운드 `8080`이 안 열림
  - 다른 인스턴스에 배포함
- CORS 에러:
  - `.env`의 `CORS_ALLOWED_ORIGINS`에 Vercel 도메인이 빠짐
- DB 에러:
  - RDS 보안 그룹이 EC2 보안 그룹 또는 EC2 private IP를 허용하지 않음
  - `.env`의 `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`가 틀림
  - `SPRING_JPA_HIBERNATE_DDL_AUTO=update`가 빠져 테이블이 없음
- API 분석/위치 기능 실패:
  - `KAKAO_REST_API_KEY` 또는 `GEMINI_API_KEY`가 비어 있음
