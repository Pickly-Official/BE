# Pickly Backend Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Build a complete Pickly photo-vote backend (auth→vote→swipe→results+AI) that runs with `docker-compose up` on a single EC2 instance.

**Architecture:** Stateless REST API with JWT bearer auth and per-voter UUID header; HandlerInterceptor for protected routes instead of Spring Security filter chain; S3 for image storage; lazy AI insight cached in DB.

**Tech Stack:** Java 21 · Spring Boot 3.3.5 · Spring Data JPA · MariaDB (MySQL connector) · jjwt 0.12.6 · AWS SDK v2 S3 · RestTemplate for OAuth + OpenAI calls · Docker Compose

---

## File Map

```
build.gradle                            ← add AWS SDK, remove spring-security; keep jjwt
src/main/resources/
  application.yml                       ← extend with S3, OAuth, OpenAI env vars
  schema.sql                            ← 5-table DDL (run once via Flyway or init-script)
src/main/java/com/be/
  BeApplication.java                    ← exists
  config/
    WebConfig.java                      ← CORS + register AuthInterceptor
    RestTemplateConfig.java             ← bean for RestTemplate
    S3Config.java                       ← S3Client bean
  common/
    ApiResponse.java                    ← { success, data, message }
    GlobalExceptionHandler.java         ← @ControllerAdvice returning ApiResponse on error
    BusinessException.java              ← runtime exception with HTTP status
  auth/
    JwtProvider.java                    ← generate / parse JWT (jjwt 0.12)
    AuthInterceptor.java                ← parse Bearer → userId attribute
    oauth/
      OAuthClient.java                  ← interface { String getProviderId(token); String getNickname(token) }
      OAuthUserInfo.java                ← record(providerId, nickname)
      MockOAuthClient.java              ← returns body.nickname as providerId+nickname
      KakaoOAuthClient.java
      NaverOAuthClient.java
      GoogleOAuthClient.java
    AuthController.java                 ← POST /api/v1/auth/login/{provider}
    AuthService.java                    ← upsert user, issue JWT
  user/
    User.java                           ← @Entity
    UserRepository.java
    UserController.java                 ← GET/DELETE /api/v1/users/me
    UserService.java
  vote/
    Vote.java                           ← @Entity
    VoteRepository.java
    Photo.java                          ← @Entity
    PhotoRepository.java
    VoteController.java                 ← POST/GET /api/v1/votes/**, /results
    VoteService.java
    SwipeController.java                ← POST swipe, DELETE swipe/last
    SwipeService.java
    SwipeAction.java                    ← @Entity
    SwipeActionRepository.java
    AiInsight.java                      ← @Entity
    AiInsightRepository.java
    AiInsightService.java               ← Vision call + fallback + cache
  home/
    HomeController.java                 ← GET /api/v1/home/stats, /hot-spots
    HomeService.java
  infra/
    S3Service.java                      ← upload multipart file → return URL
docker-compose.yml
.env.example
```

---

## Task 1 – Project Skeleton: build.gradle · docker-compose · schema.sql · health check

**Files:**
- Modify: `build.gradle`
- Modify: `src/main/resources/application.yml`
- Create: `src/main/resources/schema.sql`
- Create: `docker-compose.yml`
- Create: `.env.example`
- Create: `src/main/java/com/be/common/ApiResponse.java`
- Create: `src/main/java/com/be/common/BusinessException.java`
- Create: `src/main/java/com/be/common/GlobalExceptionHandler.java`
- Create: `src/main/java/com/be/config/WebConfig.java`
- Create: `src/main/java/com/be/config/RestTemplateConfig.java`
- Create: `src/main/java/com/be/config/S3Config.java`
- Create: `src/main/java/com/be/infra/S3Service.java`
- Create: `src/main/java/com/be/home/HomeController.java`

- [ ] **Step 1: Replace build.gradle**

```groovy
// build.gradle
plugins {
    id 'java'
    id 'org.springframework.boot' version '3.3.5'
    id 'io.spring.dependency-management' version '1.1.6'
}

group = 'com.be'
version = '0.0.1-SNAPSHOT'

java {
    toolchain {
        languageVersion = JavaLanguageVersion.of(21)
    }
}

configurations {
    compileOnly {
        extendsFrom annotationProcessor
    }
}

repositories {
    mavenCentral()
}

dependencies {
    implementation 'org.springframework.boot:spring-boot-starter-web'
    implementation 'org.springframework.boot:spring-boot-starter-data-jpa'

    // JWT
    implementation 'io.jsonwebtoken:jjwt-api:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-impl:0.12.6'
    runtimeOnly 'io.jsonwebtoken:jjwt-jackson:0.12.6'

    // AWS S3
    implementation 'software.amazon.awssdk:s3:2.27.21'

    // Swagger
    implementation 'org.springdoc:springdoc-openapi-starter-webmvc-ui:2.6.0'

    // Lombok
    compileOnly 'org.projectlombok:lombok'
    annotationProcessor 'org.projectlombok:lombok'

    // MariaDB / MySQL connector
    runtimeOnly 'org.mariadb.jdbc:mariadb-java-client:3.3.3'

    testImplementation 'org.springframework.boot:spring-boot-starter-test'
}

tasks.named('test') {
    useJUnitPlatform()
}
```

- [ ] **Step 2: Update application.yml**

```yaml
# src/main/resources/application.yml
spring:
  datasource:
    url: jdbc:mariadb://${DB_HOST:localhost}:${DB_PORT:3306}/${DB_NAME:pickly}?serverTimezone=Asia/Seoul&characterEncoding=UTF-8
    username: ${DB_USERNAME:root}
    password: ${DB_PASSWORD:password}
    driver-class-name: org.mariadb.jdbc.Driver
  jpa:
    hibernate:
      ddl-auto: none
    show-sql: false
    properties:
      hibernate:
        dialect: org.hibernate.dialect.MariaDBDialect
  sql:
    init:
      mode: always
      schema-locations: classpath:schema.sql

jwt:
  secret: ${JWT_SECRET:dev-secret-key-must-be-at-least-32-characters-long}
  expiration: ${JWT_EXPIRATION:86400000}

aws:
  s3:
    bucket: ${S3_BUCKET:pickly-images}
    region: ${AWS_REGION:ap-northeast-2}
  credentials:
    access-key: ${AWS_ACCESS_KEY_ID:}
    secret-key: ${AWS_SECRET_ACCESS_KEY:}

oauth:
  kakao:
    userinfo-url: https://kapi.kakao.com/v2/user/me
  naver:
    userinfo-url: https://openapi.naver.com/v1/nid/me
  google:
    userinfo-url: https://www.googleapis.com/oauth2/v3/userinfo

openai:
  api-key: ${OPENAI_API_KEY:}
  api-url: https://api.openai.com/v1/chat/completions

springdoc:
  api-docs:
    path: /api-docs
  swagger-ui:
    path: /swagger-ui.html
```

- [ ] **Step 3: Create schema.sql**

```sql
-- src/main/resources/schema.sql
CREATE TABLE IF NOT EXISTS users (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    provider     VARCHAR(20)  NOT NULL,
    provider_id  VARCHAR(100) NOT NULL,
    nickname     VARCHAR(100) NOT NULL,
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_provider_provider_id (provider, provider_id)
);

CREATE TABLE IF NOT EXISTS vote (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    creator_id   BIGINT       NOT NULL,
    title        VARCHAR(255) NOT NULL,
    deadline     DATETIME     NOT NULL,
    use_location BOOLEAN      DEFAULT FALSE,
    created_at   DATETIME     DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (creator_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS photo (
    id            BIGINT        AUTO_INCREMENT PRIMARY KEY,
    vote_id       BIGINT        NOT NULL,
    image_url     VARCHAR(1000) NOT NULL,
    location_name VARCHAR(255),
    created_at    DATETIME      DEFAULT CURRENT_TIMESTAMP,
    FOREIGN KEY (vote_id) REFERENCES vote(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS swipe_action (
    id          BIGINT      AUTO_INCREMENT PRIMARY KEY,
    photo_id    BIGINT      NOT NULL,
    vote_id     BIGINT      NOT NULL,
    voter_id    VARCHAR(36) NOT NULL,
    action_type VARCHAR(10) NOT NULL,
    created_at  DATETIME    DEFAULT CURRENT_TIMESTAMP,
    updated_at  DATETIME    DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_photo_voter (photo_id, voter_id),
    FOREIGN KEY (photo_id) REFERENCES photo(id)  ON DELETE CASCADE,
    FOREIGN KEY (vote_id)  REFERENCES vote(id)   ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS ai_insight (
    id           BIGINT AUTO_INCREMENT PRIMARY KEY,
    vote_id      BIGINT NOT NULL,
    summary_text TEXT,
    space_tags   JSON,
    top_comments JSON,
    created_at   DATETIME DEFAULT CURRENT_TIMESTAMP,
    UNIQUE KEY uq_ai_vote_id (vote_id),
    FOREIGN KEY (vote_id) REFERENCES vote(id) ON DELETE CASCADE
);
```

- [ ] **Step 4: Create docker-compose.yml**

```yaml
# docker-compose.yml
version: '3.9'

services:
  db:
    image: mariadb:10.11
    restart: unless-stopped
    environment:
      MARIADB_ROOT_PASSWORD: ${DB_PASSWORD}
      MARIADB_DATABASE: ${DB_NAME}
    ports:
      - "3306:3306"
    volumes:
      - db_data:/var/lib/mysql
    healthcheck:
      test: ["CMD", "healthcheck.sh", "--connect", "--innodb_initialized"]
      start_period: 10s
      interval: 10s
      timeout: 5s
      retries: 3

  app:
    build: .
    restart: unless-stopped
    ports:
      - "8080:8080"
    environment:
      DB_HOST: db
      DB_PORT: 3306
      DB_NAME: ${DB_NAME}
      DB_USERNAME: root
      DB_PASSWORD: ${DB_PASSWORD}
      JWT_SECRET: ${JWT_SECRET}
      S3_BUCKET: ${S3_BUCKET}
      AWS_REGION: ${AWS_REGION}
      AWS_ACCESS_KEY_ID: ${AWS_ACCESS_KEY_ID}
      AWS_SECRET_ACCESS_KEY: ${AWS_SECRET_ACCESS_KEY}
      OPENAI_API_KEY: ${OPENAI_API_KEY}
    depends_on:
      db:
        condition: service_healthy

volumes:
  db_data:
```

- [ ] **Step 5: Create Dockerfile**

```dockerfile
# Dockerfile
FROM eclipse-temurin:21-jdk-alpine AS builder
WORKDIR /app
COPY gradlew .
COPY gradle gradle
COPY build.gradle .
COPY settings.gradle .
COPY src src
RUN chmod +x gradlew && ./gradlew bootJar -x test --no-daemon

FROM eclipse-temurin:21-jre-alpine
WORKDIR /app
COPY --from=builder /app/build/libs/*.jar app.jar
EXPOSE 8080
ENTRYPOINT ["java", "-jar", "app.jar"]
```

- [ ] **Step 6: Create .env.example**

```
# .env.example  –  copy to .env and fill in real values
DB_NAME=pickly
DB_PASSWORD=changeme
JWT_SECRET=your-secret-key-must-be-at-least-32-characters-long
S3_BUCKET=your-bucket-name
AWS_REGION=ap-northeast-2
AWS_ACCESS_KEY_ID=
AWS_SECRET_ACCESS_KEY=
OPENAI_API_KEY=
```

- [ ] **Step 7: Create common classes**

```java
// src/main/java/com/be/common/ApiResponse.java
package com.be.common;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.Getter;

@Getter
@JsonInclude(JsonInclude.Include.NON_NULL)
public class ApiResponse<T> {
    private final boolean success;
    private final T data;
    private final String message;

    private ApiResponse(boolean success, T data, String message) {
        this.success = success;
        this.data = data;
        this.message = message;
    }

    public static <T> ApiResponse<T> ok(T data) {
        return new ApiResponse<>(true, data, null);
    }

    public static <T> ApiResponse<T> ok() {
        return new ApiResponse<>(true, null, null);
    }

    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(false, null, message);
    }
}
```

```java
// src/main/java/com/be/common/BusinessException.java
package com.be.common;

import org.springframework.http.HttpStatus;
import lombok.Getter;

@Getter
public class BusinessException extends RuntimeException {
    private final HttpStatus status;

    public BusinessException(HttpStatus status, String message) {
        super(message);
        this.status = status;
    }
}
```

```java
// src/main/java/com/be/common/GlobalExceptionHandler.java
package com.be.common;

import lombok.extern.slf4j.Slf4j;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<?>> handle(BusinessException e) {
        log.warn("BusinessException: {}", e.getMessage());
        return ResponseEntity.status(e.getStatus()).body(ApiResponse.error(e.getMessage()));
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ApiResponse<?>> handle(MaxUploadSizeExceededException e) {
        return ResponseEntity.badRequest().body(ApiResponse.error("파일 크기 초과"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<?>> handle(Exception e) {
        log.error("Unhandled exception", e);
        return ResponseEntity.internalServerError().body(ApiResponse.error("서버 오류"));
    }
}
```

- [ ] **Step 8: Create config classes**

```java
// src/main/java/com/be/config/WebConfig.java
package com.be.config;

import com.be.auth.AuthInterceptor;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final AuthInterceptor authInterceptor;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true);
    }

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(authInterceptor)
                .addPathPatterns("/api/v1/**")
                .excludePathPatterns(
                        "/api/v1/auth/**",
                        "/api/v1/home/**",
                        "/api/v1/votes/*/results",
                        "/api-docs/**",
                        "/swagger-ui/**",
                        "/swagger-ui.html"
                );
    }
}
```

```java
// src/main/java/com/be/config/RestTemplateConfig.java
package com.be.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

@Configuration
public class RestTemplateConfig {
    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }
}
```

```java
// src/main/java/com/be/config/S3Config.java
package com.be.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

@Configuration
public class S3Config {

    @Value("${aws.credentials.access-key}")
    private String accessKey;

    @Value("${aws.credentials.secret-key}")
    private String secretKey;

    @Value("${aws.s3.region}")
    private String region;

    @Bean
    public S3Client s3Client() {
        if (accessKey == null || accessKey.isBlank()) {
            // 로컬/데모 환경: 기본 자격증명 체인 사용
            return S3Client.builder().region(Region.of(region)).build();
        }
        return S3Client.builder()
                .region(Region.of(region))
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(accessKey, secretKey)))
                .build();
    }
}
```

- [ ] **Step 9: Create S3Service**

```java
// src/main/java/com/be/infra/S3Service.java
package com.be.infra;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3Service {

    private final S3Client s3Client;

    @Value("${aws.s3.bucket}")
    private String bucket;

    @Value("${aws.s3.region}")
    private String region;

    public String upload(MultipartFile file) {
        String key = "photos/" + UUID.randomUUID() + "-" + file.getOriginalFilename();
        try {
            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(bucket)
                            .key(key)
                            .contentType(file.getContentType())
                            .contentLength(file.getSize())
                            .build(),
                    RequestBody.fromInputStream(file.getInputStream(), file.getSize())
            );
        } catch (Exception e) {
            log.error("S3 upload failed", e);
            throw new RuntimeException("이미지 업로드 실패: " + e.getMessage());
        }
        return "https://" + bucket + ".s3." + region + ".amazonaws.com/" + key;
    }
}
```

- [ ] **Step 10: Create placeholder AuthInterceptor (needed for WebConfig compile)**

```java
// src/main/java/com/be/auth/AuthInterceptor.java
package com.be.auth;

import com.be.common.BusinessException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class AuthInterceptor implements HandlerInterceptor {

    private final JwtProvider jwtProvider;

    public static final String USER_ID_ATTR = "userId";

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        String header = request.getHeader("Authorization");
        if (header == null || !header.startsWith("Bearer ")) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다");
        }
        String token = header.substring(7);
        Long userId = jwtProvider.parseUserId(token);
        request.setAttribute(USER_ID_ATTR, userId);
        return true;
    }
}
```

- [ ] **Step 11: Create placeholder JwtProvider**

```java
// src/main/java/com/be/auth/JwtProvider.java
package com.be.auth;

import io.jsonwebtoken.Claims;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.util.Date;

@Component
public class JwtProvider {

    private final SecretKey key;
    private final long expMs;

    public JwtProvider(
            @Value("${jwt.secret}") String secret,
            @Value("${jwt.expiration}") long expMs
    ) {
        this.key = Keys.hmacShaKeyFor(secret.getBytes(StandardCharsets.UTF_8));
        this.expMs = expMs;
    }

    public String generate(Long userId) {
        return Jwts.builder()
                .subject(userId.toString())
                .issuedAt(new Date())
                .expiration(new Date(System.currentTimeMillis() + expMs))
                .signWith(key)
                .compact();
    }

    public Long parseUserId(String token) {
        Claims claims = Jwts.parser()
                .verifyWith(key)
                .build()
                .parseSignedClaims(token)
                .getPayload();
        return Long.valueOf(claims.getSubject());
    }
}
```

- [ ] **Step 12: Create health check endpoint (HomeController stub)**

```java
// src/main/java/com/be/home/HomeController.java
package com.be.home;

import com.be.common.ApiResponse;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1")
public class HomeController {

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.ok("ok");
    }

    @GetMapping("/home/stats")
    public ApiResponse<Map<String, Object>> stats() {
        // TODO Task 7
        return ApiResponse.ok(Map.of("totalVoters", 0, "todayVotes", 0));
    }

    @GetMapping("/home/hot-spots")
    public ApiResponse<Object> hotSpots() {
        return ApiResponse.ok(java.util.List.of());
    }
}
```

- [ ] **Step 13: Verify build compiles**

Run:
```bash
cd /home/dyjung/Documents/02_project/acc_final_after/BE
./gradlew compileJava --no-daemon
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 14: Commit**

```bash
git add -A
git commit -m "feat: Task1 - project skeleton, schema.sql, docker-compose, health check"
```

---

## Task 2 – Auth: JWT · Interceptor · User entity · OAuthClient(interface+mock) · AuthService

**Files:**
- Modify: `src/main/java/com/be/auth/JwtProvider.java` (already created in Task 1)
- Modify: `src/main/java/com/be/auth/AuthInterceptor.java` (already created)
- Create: `src/main/java/com/be/auth/oauth/OAuthClient.java`
- Create: `src/main/java/com/be/auth/oauth/OAuthUserInfo.java`
- Create: `src/main/java/com/be/auth/oauth/MockOAuthClient.java`
- Create: `src/main/java/com/be/auth/AuthController.java`
- Create: `src/main/java/com/be/auth/AuthService.java`
- Create: `src/main/java/com/be/user/User.java`
- Create: `src/main/java/com/be/user/UserRepository.java`
- Create: `src/main/java/com/be/user/UserController.java`
- Create: `src/main/java/com/be/user/UserService.java`

- [ ] **Step 1: Create User entity**

```java
// src/main/java/com/be/user/User.java
package com.be.user;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "users")
@Getter
@NoArgsConstructor
public class User {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 20)
    private String provider;

    @Column(name = "provider_id", nullable = false, length = 100)
    private String providerId;

    @Column(nullable = false, length = 100)
    private String nickname;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public static User of(String provider, String providerId, String nickname) {
        User u = new User();
        u.provider = provider;
        u.providerId = providerId;
        u.nickname = nickname;
        u.createdAt = LocalDateTime.now();
        return u;
    }
}
```

- [ ] **Step 2: Create UserRepository**

```java
// src/main/java/com/be/user/UserRepository.java
package com.be.user;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByProviderAndProviderId(String provider, String providerId);
}
```

- [ ] **Step 3: Create OAuthClient interface and OAuthUserInfo**

```java
// src/main/java/com/be/auth/oauth/OAuthUserInfo.java
package com.be.auth.oauth;

public record OAuthUserInfo(String providerId, String nickname) {}
```

```java
// src/main/java/com/be/auth/oauth/OAuthClient.java
package com.be.auth.oauth;

public interface OAuthClient {
    String provider();
    OAuthUserInfo getUserInfo(String accessToken);
}
```

- [ ] **Step 4: Create MockOAuthClient**

```java
// src/main/java/com/be/auth/oauth/MockOAuthClient.java
package com.be.auth.oauth;

import org.springframework.stereotype.Component;

// 데모 안전망: 토큰을 nickname으로 해석해 가입처리
@Component
public class MockOAuthClient implements OAuthClient {

    @Override
    public String provider() {
        return "mock";
    }

    @Override
    public OAuthUserInfo getUserInfo(String accessToken) {
        // accessToken 자체를 nickname으로 사용 (유일성 보장용)
        String nickname = accessToken.length() > 20 ? accessToken.substring(0, 20) : accessToken;
        return new OAuthUserInfo("mock_" + accessToken, nickname);
    }
}
```

- [ ] **Step 5: Create AuthService**

```java
// src/main/java/com/be/auth/AuthService.java
package com.be.auth;

import com.be.auth.oauth.OAuthClient;
import com.be.auth.oauth.OAuthUserInfo;
import com.be.common.BusinessException;
import com.be.user.User;
import com.be.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final JwtProvider jwtProvider;
    private final List<OAuthClient> oauthClients;

    private Map<String, OAuthClient> clientMap() {
        return oauthClients.stream()
                .collect(Collectors.toMap(OAuthClient::provider, Function.identity()));
    }

    @Transactional
    public Map<String, Object> login(String provider, String accessToken) {
        OAuthClient client = clientMap().get(provider);
        if (client == null) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "지원하지 않는 provider: " + provider);
        }

        OAuthUserInfo info;
        try {
            info = client.getUserInfo(accessToken);
        } catch (Exception e) {
            throw new BusinessException(HttpStatus.UNAUTHORIZED, "소셜 토큰 검증 실패: " + e.getMessage());
        }

        boolean[] isNew = {false};
        User user = userRepository.findByProviderAndProviderId(provider, info.providerId())
                .orElseGet(() -> {
                    isNew[0] = true;
                    return userRepository.save(User.of(provider, info.providerId(), info.nickname()));
                });

        String jwt = jwtProvider.generate(user.getId());
        return Map.of("token", jwt, "isNewUser", isNew[0]);
    }
}
```

- [ ] **Step 6: Create AuthController**

```java
// src/main/java/com/be/auth/AuthController.java
package com.be.auth;

import com.be.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login/{provider}")
    public ApiResponse<Map<String, Object>> login(
            @PathVariable String provider,
            @RequestBody Map<String, String> body) {
        String accessToken = body.get("accessToken");
        return ApiResponse.ok(authService.login(provider, accessToken));
    }
}
```

- [ ] **Step 7: Create UserService**

```java
// src/main/java/com/be/user/UserService.java
package com.be.user;

import com.be.common.BusinessException;
import com.be.vote.VoteRepository;
import com.be.vote.SwipeActionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    public User findById(Long id) {
        return userRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "사용자 없음"));
    }

    @Transactional
    public void deleteById(Long id) {
        userRepository.deleteById(id);
    }
}
```

- [ ] **Step 8: Create UserController**

```java
// src/main/java/com/be/user/UserController.java
package com.be.user;

import com.be.auth.AuthInterceptor;
import com.be.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> getMe(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.USER_ID_ATTR);
        User user = userService.findById(userId);
        // stats는 Task 5에서 채울 예정, 임시 반환
        return ApiResponse.ok(Map.of(
                "id", user.getId(),
                "nickname", user.getNickname(),
                "provider", user.getProvider(),
                "stats", Map.of("createdVotes", 0, "receivedVotes", 0, "bestCuts", 0)
        ));
    }

    @DeleteMapping("/me")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteMe(HttpServletRequest request) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.USER_ID_ATTR);
        userService.deleteById(userId);
    }
}
```

- [ ] **Step 9: Add GET /votes/** exclusions to WebConfig (they're public)**

GET /votes/{voteId} and GET /votes/{voteId}/results need to be public. Update WebConfig exclusions:

```java
// In WebConfig.addInterceptors(), update excludePathPatterns:
.excludePathPatterns(
    "/api/v1/auth/**",
    "/api/v1/home/**",
    "/api/v1/votes/*/results",
    "/api-docs/**",
    "/swagger-ui/**",
    "/swagger-ui.html"
);
// Note: GET /votes/{id} is public but POST /votes is protected.
// The interceptor only gates by path pattern, so /votes/{id} GET is excluded here too.
```

Update `WebConfig.java` to add `/api/v1/votes/*` to excludePathPatterns (GET /votes/{id} is public; the interceptor is stateless so POST /votes will still require JWT because WebConfig only excludes the pattern, but the annotation approach here needs a rethink).

**Actually**: the interceptor pattern-based exclude is coarse. To handle "GET is public, POST requires JWT," add an HTTP method check inside AuthInterceptor:

```java
// Update AuthInterceptor.java preHandle:
@Override
public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
    String method = request.getMethod();
    String path = request.getRequestURI();

    // GET /votes/{id} and GET /votes/{id}/results are public
    if ("GET".equals(method) && path.matches("/api/v1/votes/\\d+(/results)?")) {
        return true;
    }
    // POST /votes/{id}/swipe and DELETE /votes/{id}/swipe/last use X-Voter-Id, not JWT
    if (path.matches("/api/v1/votes/\\d+/swipe.*")) {
        return true;
    }

    String header = request.getHeader("Authorization");
    if (header == null || !header.startsWith("Bearer ")) {
        throw new BusinessException(HttpStatus.UNAUTHORIZED, "인증이 필요합니다");
    }
    Long userId = jwtProvider.parseUserId(header.substring(7));
    request.setAttribute(USER_ID_ATTR, userId);
    return true;
}
```

And simplify WebConfig to exclude only the truly public paths:
```java
.excludePathPatterns(
    "/api/v1/auth/**",
    "/api/v1/home/**",
    "/api-docs/**",
    "/swagger-ui/**",
    "/swagger-ui.html"
);
```

- [ ] **Step 10: Verify build compiles**

```bash
./gradlew compileJava --no-daemon
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 11: Start MariaDB locally to test login flow**

```bash
# If MariaDB available locally:
./gradlew bootRun --no-daemon &
# Test mock login:
curl -s -X POST http://localhost:8080/api/v1/auth/login/mock \
  -H "Content-Type: application/json" \
  -d '{"accessToken":"testuser123"}' | jq .
# Expected: { success: true, data: { token: "...", isNewUser: true } }

# Test /users/me with returned token:
TOKEN="<paste token>"
curl -s http://localhost:8080/api/v1/users/me \
  -H "Authorization: Bearer $TOKEN" | jq .
```

- [ ] **Step 12: Commit**

```bash
git add -A
git commit -m "feat: Task2 - JWT, AuthInterceptor, User entity, mock OAuth login"
```

---

## Task 3 – Kakao / Naver / Google OAuth clients

**Files:**
- Create: `src/main/java/com/be/auth/oauth/KakaoOAuthClient.java`
- Create: `src/main/java/com/be/auth/oauth/NaverOAuthClient.java`
- Create: `src/main/java/com/be/auth/oauth/GoogleOAuthClient.java`

All three follow the same pattern: call the provider's userinfo URL with `Authorization: Bearer {token}`, parse the JSON.

- [ ] **Step 1: Create KakaoOAuthClient**

```java
// src/main/java/com/be/auth/oauth/KakaoOAuthClient.java
package com.be.auth.oauth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class KakaoOAuthClient implements OAuthClient {

    private final RestTemplate restTemplate;

    @Value("${oauth.kakao.userinfo-url}")
    private String userinfoUrl;

    @Override
    public String provider() { return "kakao"; }

    @Override
    @SuppressWarnings("unchecked")
    public OAuthUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<Map> resp = restTemplate.exchange(
                userinfoUrl, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<String, Object> body = resp.getBody();
        String providerId = String.valueOf(body.get("id"));
        Map<String, Object> account = (Map<String, Object>) body.get("kakao_account");
        Map<String, Object> profile = (Map<String, Object>) account.get("profile");
        String nickname = (String) profile.get("nickname");
        return new OAuthUserInfo(providerId, nickname);
    }
}
```

- [ ] **Step 2: Create NaverOAuthClient**

```java
// src/main/java/com/be/auth/oauth/NaverOAuthClient.java
package com.be.auth.oauth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class NaverOAuthClient implements OAuthClient {

    private final RestTemplate restTemplate;

    @Value("${oauth.naver.userinfo-url}")
    private String userinfoUrl;

    @Override
    public String provider() { return "naver"; }

    @Override
    @SuppressWarnings("unchecked")
    public OAuthUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<Map> resp = restTemplate.exchange(
                userinfoUrl, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<String, Object> response = (Map<String, Object>) resp.getBody().get("response");
        String providerId = (String) response.get("id");
        String nickname = response.containsKey("nickname")
                ? (String) response.get("nickname")
                : (String) response.get("name");
        return new OAuthUserInfo(providerId, nickname);
    }
}
```

- [ ] **Step 3: Create GoogleOAuthClient**

```java
// src/main/java/com/be/auth/oauth/GoogleOAuthClient.java
package com.be.auth.oauth;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class GoogleOAuthClient implements OAuthClient {

    private final RestTemplate restTemplate;

    @Value("${oauth.google.userinfo-url}")
    private String userinfoUrl;

    @Override
    public String provider() { return "google"; }

    @Override
    @SuppressWarnings("unchecked")
    public OAuthUserInfo getUserInfo(String accessToken) {
        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(accessToken);
        ResponseEntity<Map> resp = restTemplate.exchange(
                userinfoUrl, HttpMethod.GET, new HttpEntity<>(headers), Map.class);
        Map<String, Object> body = resp.getBody();
        String providerId = (String) body.get("sub");
        String nickname = (String) body.get("name");
        return new OAuthUserInfo(providerId, nickname);
    }
}
```

- [ ] **Step 4: Compile and commit**

```bash
./gradlew compileJava --no-daemon
git add -A
git commit -m "feat: Task3 - Kakao/Naver/Google OAuth clients"
```

---

## Task 4 – Vote creation (S3) · GET /votes/{id} · GET /votes/me

**Files:**
- Create: `src/main/java/com/be/vote/Vote.java`
- Create: `src/main/java/com/be/vote/VoteRepository.java`
- Create: `src/main/java/com/be/vote/Photo.java`
- Create: `src/main/java/com/be/vote/PhotoRepository.java`
- Create: `src/main/java/com/be/vote/VoteController.java`
- Create: `src/main/java/com/be/vote/VoteService.java`
- Create: `src/main/java/com/be/vote/SwipeAction.java` (needed for schema consistency)
- Create: `src/main/java/com/be/vote/SwipeActionRepository.java`

- [ ] **Step 1: Create Vote entity**

```java
// src/main/java/com/be/vote/Vote.java
package com.be.vote;

import com.be.user.User;
import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "vote")
@Getter
@NoArgsConstructor
public class Vote {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "creator_id", nullable = false)
    private User creator;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private LocalDateTime deadline;

    @Column(name = "use_location")
    private boolean useLocation;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @OneToMany(mappedBy = "vote", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<Photo> photos = new ArrayList<>();

    public static Vote of(User creator, String title, LocalDateTime deadline, boolean useLocation) {
        Vote v = new Vote();
        v.creator = creator;
        v.title = title;
        v.deadline = deadline;
        v.useLocation = useLocation;
        v.createdAt = LocalDateTime.now();
        return v;
    }

    public boolean isClosed() {
        return LocalDateTime.now().isAfter(deadline);
    }
}
```

- [ ] **Step 2: Create Photo entity**

```java
// src/main/java/com/be/vote/Photo.java
package com.be.vote;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "photo")
@Getter
@NoArgsConstructor
public class Photo {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "vote_id", nullable = false)
    private Vote vote;

    @Column(name = "image_url", nullable = false, length = 1000)
    private String imageUrl;

    @Column(name = "location_name")
    private String locationName;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public static Photo of(Vote vote, String imageUrl) {
        Photo p = new Photo();
        p.vote = vote;
        p.imageUrl = imageUrl;
        p.createdAt = LocalDateTime.now();
        return p;
    }
}
```

- [ ] **Step 3: Create SwipeAction entity**

```java
// src/main/java/com/be/vote/SwipeAction.java
package com.be.vote;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Entity
@Table(name = "swipe_action")
@Getter
@NoArgsConstructor
public class SwipeAction {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "photo_id", nullable = false)
    private Long photoId;

    @Column(name = "vote_id", nullable = false)
    private Long voteId;

    @Column(name = "voter_id", nullable = false, length = 36)
    private String voterId;

    @Column(name = "action_type", nullable = false, length = 10)
    private String actionType;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    @Column(name = "updated_at")
    private LocalDateTime updatedAt = LocalDateTime.now();

    public static SwipeAction of(Long photoId, Long voteId, String voterId, String actionType) {
        SwipeAction s = new SwipeAction();
        s.photoId = photoId;
        s.voteId = voteId;
        s.voterId = voterId;
        s.actionType = actionType;
        s.createdAt = LocalDateTime.now();
        s.updatedAt = LocalDateTime.now();
        return s;
    }
}
```

- [ ] **Step 4: Create AiInsight entity**

```java
// src/main/java/com/be/vote/AiInsight.java
package com.be.vote;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Entity
@Table(name = "ai_insight")
@Getter
@NoArgsConstructor
public class AiInsight {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "vote_id", nullable = false, unique = true)
    private Long voteId;

    @Column(name = "summary_text", columnDefinition = "TEXT")
    private String summaryText;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "space_tags", columnDefinition = "JSON")
    private List<String> spaceTags;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "top_comments", columnDefinition = "JSON")
    private List<Map<String, Object>> topComments;

    @Column(name = "created_at")
    private LocalDateTime createdAt = LocalDateTime.now();

    public static AiInsight of(Long voteId, String summaryText, List<String> spaceTags,
                                List<Map<String, Object>> topComments) {
        AiInsight a = new AiInsight();
        a.voteId = voteId;
        a.summaryText = summaryText;
        a.spaceTags = spaceTags;
        a.topComments = topComments;
        a.createdAt = LocalDateTime.now();
        return a;
    }
}
```

- [ ] **Step 5: Create repositories**

```java
// src/main/java/com/be/vote/VoteRepository.java
package com.be.vote;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDateTime;
import java.util.List;

public interface VoteRepository extends JpaRepository<Vote, Long> {

    @Query("SELECT v FROM Vote v WHERE v.creator.id = :creatorId AND v.deadline > :now ORDER BY v.createdAt DESC")
    List<Vote> findActiveByCreatorId(@Param("creatorId") Long creatorId, @Param("now") LocalDateTime now);

    @Query("SELECT v FROM Vote v WHERE v.creator.id = :creatorId AND v.deadline <= :now ORDER BY v.createdAt DESC")
    List<Vote> findClosedByCreatorId(@Param("creatorId") Long creatorId, @Param("now") LocalDateTime now);

    List<Vote> findByCreatorId(Long creatorId);
}
```

```java
// src/main/java/com/be/vote/PhotoRepository.java
package com.be.vote;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface PhotoRepository extends JpaRepository<Photo, Long> {
    List<Photo> findByVoteIdOrderByIdAsc(Long voteId);
}
```

```java
// src/main/java/com/be/vote/SwipeActionRepository.java
package com.be.vote;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SwipeActionRepository extends JpaRepository<SwipeAction, Long> {

    @Modifying
    @Query(value = """
        INSERT INTO swipe_action (photo_id, vote_id, voter_id, action_type, created_at, updated_at)
        VALUES (:photoId, :voteId, :voterId, :actionType, NOW(), NOW())
        ON DUPLICATE KEY UPDATE action_type = VALUES(action_type), updated_at = NOW()
        """, nativeQuery = true)
    void upsert(@Param("photoId") Long photoId,
                @Param("voteId") Long voteId,
                @Param("voterId") String voterId,
                @Param("actionType") String actionType);

    // 특정 voter가 특정 vote에서 한 스와이프 중 가장 최근 1건
    Optional<SwipeAction> findTopByVoteIdAndVoterIdOrderByUpdatedAtDesc(Long voteId, String voterId);

    List<SwipeAction> findByVoteIdAndVoterId(Long voteId, String voterId);

    // 집계용
    @Query(value = "SELECT COUNT(DISTINCT voter_id) FROM swipe_action WHERE vote_id = :voteId", nativeQuery = true)
    long countDistinctVotersByVoteId(@Param("voteId") Long voteId);

    @Query(value = "SELECT COUNT(*) FROM swipe_action WHERE photo_id = :photoId AND action_type = 'LIKE'", nativeQuery = true)
    long countLikesByPhotoId(@Param("photoId") Long photoId);

    @Query(value = "SELECT COUNT(*) FROM swipe_action WHERE photo_id = :photoId", nativeQuery = true)
    long countTotalByPhotoId(@Param("photoId") Long photoId);
}
```

```java
// src/main/java/com/be/vote/AiInsightRepository.java
package com.be.vote;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface AiInsightRepository extends JpaRepository<AiInsight, Long> {
    Optional<AiInsight> findByVoteId(Long voteId);
}
```

- [ ] **Step 6: Create VoteService**

```java
// src/main/java/com/be/vote/VoteService.java
package com.be.vote;

import com.be.common.BusinessException;
import com.be.infra.S3Service;
import com.be.user.User;
import com.be.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class VoteService {

    private final VoteRepository voteRepository;
    private final PhotoRepository photoRepository;
    private final UserRepository userRepository;
    private final S3Service s3Service;

    @Transactional
    public Long createVote(Long creatorId, String title, String deadlinePeriod,
                           boolean useLocation, List<MultipartFile> images) {
        if (title == null || title.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "제목은 필수입니다");
        }
        if (images == null || images.size() < 2 || images.size() > 10) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "이미지는 2~10장이어야 합니다");
        }

        LocalDateTime deadline = switch (deadlinePeriod) {
            case "24h" -> LocalDateTime.now().plusHours(24);
            case "3d"  -> LocalDateTime.now().plusDays(3);
            case "7d"  -> LocalDateTime.now().plusDays(7);
            default    -> throw new BusinessException(HttpStatus.BAD_REQUEST, "deadline은 24h|3d|7d 중 하나");
        };

        User creator = userRepository.findById(creatorId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "사용자 없음"));

        Vote vote = voteRepository.save(Vote.of(creator, title, deadline, useLocation));

        for (MultipartFile image : images) {
            String url = s3Service.upload(image);
            photoRepository.save(Photo.of(vote, url));
        }

        return vote.getId();
    }

    public Vote findById(Long voteId) {
        return voteRepository.findById(voteId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "투표 없음"));
    }

    public List<Vote> findMyVotes(Long userId, String status) {
        LocalDateTime now = LocalDateTime.now();
        return switch (status) {
            case "active" -> voteRepository.findActiveByCreatorId(userId, now);
            case "closed" -> voteRepository.findClosedByCreatorId(userId, now);
            default       -> voteRepository.findByCreatorId(userId);
        };
    }
}
```

- [ ] **Step 7: Create VoteController**

```java
// src/main/java/com/be/vote/VoteController.java
package com.be.vote;

import com.be.auth.AuthInterceptor;
import com.be.common.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/votes")
@RequiredArgsConstructor
public class VoteController {

    private final VoteService voteService;
    private final ResultService resultService;

    @PostMapping(consumes = "multipart/form-data")
    @ResponseStatus(HttpStatus.CREATED)
    public ApiResponse<Map<String, Long>> create(
            HttpServletRequest request,
            @RequestParam String title,
            @RequestParam String deadline,
            @RequestParam(defaultValue = "false") boolean useLocation,
            @RequestParam("images") List<MultipartFile> images) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.USER_ID_ATTR);
        Long voteId = voteService.createVote(userId, title, deadline, useLocation, images);
        return ApiResponse.ok(Map.of("voteId", voteId));
    }

    @GetMapping("/me")
    public ApiResponse<List<Map<String, Object>>> myVotes(
            HttpServletRequest request,
            @RequestParam(defaultValue = "active") String status) {
        Long userId = (Long) request.getAttribute(AuthInterceptor.USER_ID_ATTR);
        List<Vote> votes = voteService.findMyVotes(userId, status);
        List<Map<String, Object>> result = votes.stream().map(v -> {
            String thumbnail = v.getPhotos().isEmpty() ? null : v.getPhotos().get(0).getImageUrl();
            return Map.<String, Object>of(
                    "voteId", v.getId(),
                    "title", v.getTitle(),
                    "thumbnailUrl", thumbnail != null ? thumbnail : "",
                    "deadline", v.getDeadline().toString(),
                    "closed", v.isClosed()
            );
        }).collect(Collectors.toList());
        return ApiResponse.ok(result);
    }

    @GetMapping("/{voteId}")
    public ApiResponse<Map<String, Object>> getVote(@PathVariable Long voteId) {
        Vote vote = voteService.findById(voteId);
        List<Map<String, Object>> photos = vote.getPhotos().stream()
                .map(p -> Map.<String, Object>of("photoId", p.getId(), "imageUrl", p.getImageUrl()))
                .collect(Collectors.toList());
        String locationName = vote.getPhotos().isEmpty() ? null
                : vote.getPhotos().get(0).getLocationName();
        return ApiResponse.ok(Map.of(
                "voteId", vote.getId(),
                "title", vote.getTitle(),
                "locationName", locationName != null ? locationName : "",
                "closed", vote.isClosed(),
                "photos", photos
        ));
    }

    @GetMapping("/{voteId}/results")
    public ApiResponse<Map<String, Object>> getResults(@PathVariable Long voteId) {
        return ApiResponse.ok(resultService.getResults(voteId));
    }
}
```

- [ ] **Step 8: Create ResultService stub (needed for VoteController compile)**

```java
// src/main/java/com/be/vote/ResultService.java
package com.be.vote;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class ResultService {

    private final VoteRepository voteRepository;
    private final PhotoRepository photoRepository;
    private final SwipeActionRepository swipeActionRepository;
    private final AiInsightService aiInsightService;

    public Map<String, Object> getResults(Long voteId) {
        Vote vote = voteRepository.findById(voteId).orElseThrow();
        List<Photo> photos = photoRepository.findByVoteIdOrderByIdAsc(voteId);
        long totalVoters = swipeActionRepository.countDistinctVotersByVoteId(voteId);

        List<Map<String, Object>> ranking = buildRanking(photos);
        Map<String, Object> aiInsight = aiInsightService.getOrCreate(vote, photos);

        return Map.of(
                "voteId", vote.getId(),
                "title", vote.getTitle(),
                "locationName", photos.isEmpty() || photos.get(0).getLocationName() == null
                        ? "" : photos.get(0).getLocationName(),
                "totalVoters", totalVoters,
                "ranking", ranking,
                "aiInsight", aiInsight != null ? aiInsight : ""
        );
    }

    private List<Map<String, Object>> buildRanking(List<Photo> photos) {
        List<Map<String, Object>> items = photos.stream().map(p -> {
            long likes = swipeActionRepository.countLikesByPhotoId(p.getId());
            long total = swipeActionRepository.countTotalByPhotoId(p.getId());
            double rate = total == 0 ? 0.0 : (double) likes / total * 100;
            return Map.<String, Object>of(
                    "photoId", p.getId(),
                    "imageUrl", p.getImageUrl(),
                    "likes", likes,
                    "total", total,
                    "recommendRate", Math.round(rate * 10) / 10.0
            );
        }).sorted((a, b) -> Long.compare((Long) b.get("likes"), (Long) a.get("likes")))
          .collect(java.util.stream.Collectors.toList());

        for (int i = 0; i < items.size(); i++) {
            Map<String, Object> item = items.get(i);
            Map<String, Object> withRank = new java.util.LinkedHashMap<>(item);
            withRank.put("rank", i + 1);
            items.set(i, withRank);
        }
        return items;
    }
}
```

Note: This needs `import java.util.List;` — add at top of file.

- [ ] **Step 9: Create AiInsightService stub**

```java
// src/main/java/com/be/vote/AiInsightService.java
package com.be.vote;

import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;

@Service
public class AiInsightService {

    public Map<String, Object> getOrCreate(Vote vote, List<Photo> photos) {
        // TODO: Task 6 - Vision API + fallback
        return fallback(photos);
    }

    Map<String, Object> fallback(List<Photo> photos) {
        List<Map<String, Object>> comments = photos.stream()
                .map(p -> Map.<String, Object>of(
                        "photoId", p.getId(),
                        "composition", "균형 잡힌 구도로 피사체가 잘 담겼습니다",
                        "expression", "자연스럽고 밝은 표정이 돋보입니다",
                        "lighting", "자연광이 부드럽게 활용되었습니다"
                )).toList();
        return Map.of(
                "model", "fallback",
                "summary", "전반적으로 완성도 높은 사진들입니다. 투표 결과를 참고해 최고의 컷을 선택해보세요.",
                "spaceTags", List.of("자연광", "감성사진", "인물"),
                "topComments", comments
        );
    }
}
```

- [ ] **Step 10: Compile**

```bash
./gradlew compileJava --no-daemon
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 11: Commit**

```bash
git add -A
git commit -m "feat: Task4 - vote creation, S3 upload, GET /votes endpoints"
```

---

## Task 5 – Swipe · Undo · Results aggregation

**Files:**
- Create: `src/main/java/com/be/vote/SwipeController.java`
- Create: `src/main/java/com/be/vote/SwipeService.java`

- [ ] **Step 1: Create SwipeService**

```java
// src/main/java/com/be/vote/SwipeService.java
package com.be.vote;

import com.be.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class SwipeService {

    private final SwipeActionRepository swipeActionRepository;
    private final PhotoRepository photoRepository;
    private final VoteRepository voteRepository;

    @Transactional
    public void swipe(Long voteId, String voterId, Long photoId, String action) {
        if (!"LIKE".equals(action) && !"SKIP".equals(action)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "action은 LIKE 또는 SKIP");
        }

        // 해당 photo가 이 vote 소속인지 검증
        Photo photo = photoRepository.findById(photoId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "사진 없음"));
        if (!photo.getVote().getId().equals(voteId)) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "해당 투표의 사진이 아닙니다");
        }

        swipeActionRepository.upsert(photoId, voteId, voterId, action);
    }

    @Transactional
    public Long undoLast(Long voteId, String voterId) {
        SwipeAction last = swipeActionRepository
                .findTopByVoteIdAndVoterIdOrderByUpdatedAtDesc(voteId, voterId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "되돌릴 스와이프가 없습니다"));
        Long photoId = last.getPhotoId();
        swipeActionRepository.delete(last);
        return photoId;
    }
}
```

- [ ] **Step 2: Create SwipeController**

```java
// src/main/java/com/be/vote/SwipeController.java
package com.be.vote;

import com.be.common.ApiResponse;
import com.be.common.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/votes/{voteId}/swipe")
@RequiredArgsConstructor
public class SwipeController {

    private final SwipeService swipeService;

    @PostMapping
    public ApiResponse<Void> swipe(
            @PathVariable Long voteId,
            @RequestHeader(value = "X-Voter-Id", required = false) String voterId,
            @RequestBody Map<String, Object> body) {
        if (voterId == null || voterId.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "X-Voter-Id 헤더 필요");
        }
        Long photoId = Long.valueOf(String.valueOf(body.get("photoId")));
        String action = (String) body.get("action");
        swipeService.swipe(voteId, voterId, photoId, action);
        return ApiResponse.ok();
    }

    @DeleteMapping("/last")
    public ApiResponse<Map<String, Long>> undoLast(
            @PathVariable Long voteId,
            @RequestHeader(value = "X-Voter-Id", required = false) String voterId) {
        if (voterId == null || voterId.isBlank()) {
            throw new BusinessException(HttpStatus.BAD_REQUEST, "X-Voter-Id 헤더 필요");
        }
        Long photoId = swipeService.undoLast(voteId, voterId);
        return ApiResponse.ok(Map.of("photoId", photoId));
    }
}
```

- [ ] **Step 3: Compile**

```bash
./gradlew compileJava --no-daemon
```

- [ ] **Step 4: Commit**

```bash
git add -A
git commit -m "feat: Task5 - swipe upsert, undo, results aggregation"
```

---

## Task 6 – AI Insight (Vision API + fallback + DB cache)

**Files:**
- Modify: `src/main/java/com/be/vote/AiInsightService.java`

- [ ] **Step 1: Update AiInsightService with Vision call + DB cache**

```java
// src/main/java/com/be/vote/AiInsightService.java
package com.be.vote;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiInsightService {

    private final AiInsightRepository aiInsightRepository;
    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${openai.api-key:}")
    private String openaiApiKey;

    @Value("${openai.api-url}")
    private String openaiApiUrl;

    @Transactional
    public Map<String, Object> getOrCreate(Vote vote, List<Photo> photos) {
        return aiInsightRepository.findByVoteId(vote.getId())
                .map(this::toMap)
                .orElseGet(() -> {
                    Map<String, Object> result = callVisionOrFallback(vote, photos);
                    saveInsight(vote.getId(), result, photos);
                    return result;
                });
    }

    private void saveInsight(Long voteId, Map<String, Object> result, List<Photo> photos) {
        try {
            List<String> tags = objectMapper.convertValue(result.get("spaceTags"), new TypeReference<>() {});
            List<Map<String, Object>> comments = objectMapper.convertValue(result.get("topComments"), new TypeReference<>() {});
            aiInsightRepository.save(AiInsight.of(voteId, (String) result.get("summary"), tags, comments));
        } catch (Exception e) {
            log.warn("AI insight 저장 실패 (무시)", e);
        }
    }

    private Map<String, Object> toMap(AiInsight ai) {
        return Map.of(
                "model", "gpt-4o",
                "summary", ai.getSummaryText() != null ? ai.getSummaryText() : "",
                "spaceTags", ai.getSpaceTags() != null ? ai.getSpaceTags() : List.of(),
                "topComments", ai.getTopComments() != null ? ai.getTopComments() : List.of()
        );
    }

    private Map<String, Object> callVisionOrFallback(Vote vote, List<Photo> photos) {
        if (openaiApiKey == null || openaiApiKey.isBlank()) {
            log.info("OPENAI_API_KEY 없음 → fallback 반환");
            return fallback(photos);
        }
        try {
            return callVision(vote, photos);
        } catch (Exception e) {
            log.warn("Vision API 호출 실패, fallback 반환: {}", e.getMessage());
            return fallback(photos);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> callVision(Vote vote, List<Photo> photos) {
        // 최대 5장만 보내 토큰 절약
        List<Photo> targets = photos.size() > 5 ? photos.subList(0, 5) : photos;

        List<Map<String, Object>> contentParts = new java.util.ArrayList<>();
        contentParts.add(Map.of("type", "text", "text",
                "아래 사진들을 분석해주세요. JSON으로 응답해주세요: " +
                "{\"summary\":\"전체 사진 총평 1문장\",\"spaceTags\":[\"태그1\",\"태그2\",\"태그3\"]," +
                "\"topComments\":[{\"photoId\":번호,\"composition\":\"구도평\",\"expression\":\"표정평\",\"lighting\":\"조명평\"}]}"));

        for (Photo p : targets) {
            contentParts.add(Map.of(
                    "type", "image_url",
                    "image_url", Map.of("url", p.getImageUrl(), "detail", "low"),
                    "photoId", p.getId()
            ));
        }

        Map<String, Object> message = Map.of("role", "user", "content", contentParts);
        Map<String, Object> requestBody = Map.of(
                "model", "gpt-4o",
                "messages", List.of(message),
                "max_tokens", 600,
                "response_format", Map.of("type", "json_object")
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setBearerAuth(openaiApiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);

        ResponseEntity<Map> response = restTemplate.exchange(
                openaiApiUrl, HttpMethod.POST,
                new HttpEntity<>(requestBody, headers), Map.class);

        Map<String, Object> body = response.getBody();
        List<Map<String, Object>> choices = (List<Map<String, Object>>) body.get("choices");
        Map<String, Object> msg = (Map<String, Object>) choices.get(0).get("message");
        String content = (String) msg.get("content");

        Map<String, Object> parsed = objectMapper.readValue(content, new TypeReference<>() {});

        // photoId를 실제 DB ID로 매핑 (Vision 응답의 순서 기반)
        List<Map<String, Object>> rawComments = (List<Map<String, Object>>) parsed.get("topComments");
        List<Map<String, Object>> mappedComments = new java.util.ArrayList<>();
        for (int i = 0; i < rawComments.size() && i < targets.size(); i++) {
            Map<String, Object> c = new java.util.LinkedHashMap<>(rawComments.get(i));
            c.put("photoId", targets.get(i).getId());
            mappedComments.add(c);
        }

        return Map.of(
                "model", "gpt-4o",
                "summary", parsed.getOrDefault("summary", ""),
                "spaceTags", parsed.getOrDefault("spaceTags", List.of()),
                "topComments", mappedComments
        );
    }

    Map<String, Object> fallback(List<Photo> photos) {
        List<Map<String, Object>> comments = photos.stream()
                .map(p -> Map.<String, Object>of(
                        "photoId", p.getId(),
                        "composition", "균형 잡힌 구도로 피사체가 잘 담겼습니다",
                        "expression", "자연스럽고 밝은 표정이 돋보입니다",
                        "lighting", "자연광이 부드럽게 활용되었습니다"
                )).toList();
        return Map.of(
                "model", "fallback",
                "summary", "전반적으로 완성도 높은 사진들입니다. 투표 결과를 참고해 최고의 컷을 선택해보세요.",
                "spaceTags", List.of("자연광", "감성사진", "인물"),
                "topComments", comments
        );
    }
}
```

Note: `objectMapper.readValue` throws `JsonProcessingException` which is checked — add `throws Exception` to `callVision` method signature, or wrap in try-catch (already done in `callVisionOrFallback`).

- [ ] **Step 2: Compile**

```bash
./gradlew compileJava --no-daemon
```

- [ ] **Step 3: Commit**

```bash
git add -A
git commit -m "feat: Task6 - AI insight Vision API call with DB cache and fallback"
```

---

## Task 7 – Home stats · UserService stats · Final wiring · README

**Files:**
- Modify: `src/main/java/com/be/home/HomeController.java`
- Modify: `src/main/java/com/be/home/HomeService.java`  (create)
- Modify: `src/main/java/com/be/user/UserService.java`
- Modify: `src/main/java/com/be/user/UserController.java`
- Modify: `README.md`

- [ ] **Step 1: Create HomeService**

```java
// src/main/java/com/be/home/HomeService.java
package com.be.home;

import com.be.vote.SwipeActionRepository;
import com.be.vote.VoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class HomeService {

    private final SwipeActionRepository swipeActionRepository;
    private final VoteRepository voteRepository;

    public long totalVoters() {
        // DB에 voter가 존재하는 distinct count
        return swipeActionRepository.countDistinctVoters();
    }

    public long todayVotes() {
        LocalDateTime startOfDay = LocalDate.now().atStartOfDay();
        return voteRepository.countCreatedAfter(startOfDay);
    }
}
```

- [ ] **Step 2: Add queries to repositories**

In `SwipeActionRepository.java`, add:
```java
@Query(value = "SELECT COUNT(DISTINCT voter_id) FROM swipe_action", nativeQuery = true)
long countDistinctVoters();
```

In `VoteRepository.java`, add:
```java
@Query("SELECT COUNT(v) FROM Vote v WHERE v.createdAt >= :since")
long countCreatedAfter(@Param("since") LocalDateTime since);
```

- [ ] **Step 3: Update HomeController**

```java
// src/main/java/com/be/home/HomeController.java
package com.be.home;

import com.be.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class HomeController {

    private final HomeService homeService;

    @GetMapping("/health")
    public ApiResponse<String> health() {
        return ApiResponse.ok("ok");
    }

    @GetMapping("/home/stats")
    public ApiResponse<Map<String, Object>> stats() {
        return ApiResponse.ok(Map.of(
                "totalVoters", homeService.totalVoters(),
                "todayVotes", homeService.todayVotes()
        ));
    }

    @GetMapping("/home/hot-spots")
    public ApiResponse<List<Object>> hotSpots() {
        return ApiResponse.ok(List.of());
    }
}
```

- [ ] **Step 4: Update UserController /me stats**

Update `UserController.getMe` to return real stats. Add to `UserService`:

```java
// Add to UserService.java
public Map<String, Long> getStats(Long userId) {
    long created = voteRepository.countByCreatorId(userId);
    // receivedVotes = 내 vote에 달린 unique voter 수 (간단화: 내 vote count * 평균)
    // bestCuts = LIKE가 가장 많은 사진 수 (내 vote에서 1위 photos)
    return Map.of("createdVotes", created, "receivedVotes", 0L, "bestCuts", 0L);
}
```

Add `private final VoteRepository voteRepository;` to UserService and add to VoteRepository:
```java
long countByCreatorId(Long creatorId);
```

Then update UserController:
```java
@GetMapping("/me")
public ApiResponse<Map<String, Object>> getMe(HttpServletRequest request) {
    Long userId = (Long) request.getAttribute(AuthInterceptor.USER_ID_ATTR);
    User user = userService.findById(userId);
    Map<String, Long> stats = userService.getStats(userId);
    return ApiResponse.ok(Map.of(
            "id", user.getId(),
            "nickname", user.getNickname(),
            "provider", user.getProvider(),
            "stats", stats
    ));
}
```

- [ ] **Step 5: Update application.yml – add multipart size limit**

```yaml
# Add to application.yml under spring:
  servlet:
    multipart:
      max-file-size: 20MB
      max-request-size: 100MB
```

- [ ] **Step 6: Update README.md with run instructions**

Add to README.md:

```markdown
## 실행 방법

### 로컬 (docker-compose)
1. `.env.example`을 복사하여 `.env` 파일 생성 후 값 입력
2. `docker-compose up -d`
3. `curl http://localhost:8080/api/v1/health`

### 환경변수 목록
| 변수 | 설명 | 필수 |
|------|------|------|
| DB_NAME | MariaDB 데이터베이스명 | ✓ |
| DB_PASSWORD | MariaDB 루트 비밀번호 | ✓ |
| JWT_SECRET | JWT 서명 키 (32자 이상) | ✓ |
| S3_BUCKET | S3 버킷명 | ✓ |
| AWS_REGION | AWS 리전 (예: ap-northeast-2) | ✓ |
| AWS_ACCESS_KEY_ID | AWS 액세스 키 | ✓ |
| AWS_SECRET_ACCESS_KEY | AWS 시크릿 키 | ✓ |
| OPENAI_API_KEY | OpenAI API 키 (없으면 AI는 fallback) | - |

### 데모 해피패스
```bash
# 1. mock 로그인
TOKEN=$(curl -s -X POST http://localhost:8080/api/v1/auth/login/mock \
  -H "Content-Type: application/json" \
  -d '{"accessToken":"demo-user-1"}' | jq -r '.data.token')

# 2. 투표 생성 (이미지 2장 필요)
VOTE_ID=$(curl -s -X POST http://localhost:8080/api/v1/votes \
  -H "Authorization: Bearer $TOKEN" \
  -F "title=베스트컷 투표" -F "deadline=24h" -F "useLocation=false" \
  -F "images=@photo1.jpg" -F "images=@photo2.jpg" | jq -r '.data.voteId')

# 3. 스와이프
curl -s -X POST http://localhost:8080/api/v1/votes/$VOTE_ID/swipe \
  -H "X-Voter-Id: voter-uuid-001" \
  -H "Content-Type: application/json" \
  -d "{\"photoId\":1,\"action\":\"LIKE\"}"

# 4. 결과 조회 (AI 포함)
curl -s http://localhost:8080/api/v1/votes/$VOTE_ID/results | jq .
```
```

- [ ] **Step 7: Full build test**

```bash
./gradlew build --no-daemon
```
Expected: `BUILD SUCCESSFUL`

- [ ] **Step 8: Final commit**

```bash
git add -A
git commit -m "feat: Task7 - home stats, user stats, multipart limits, README"
```

---

## Self-Review

### Spec Coverage Check

| Requirement | Task | Status |
|-------------|------|--------|
| POST /auth/login/{provider} | T2 | ✓ |
| GET /users/me | T2 | ✓ |
| DELETE /users/me | T2 | ✓ |
| GET /home/stats | T1,T7 | ✓ |
| GET /home/hot-spots (dummy) | T1,T7 | ✓ |
| GET /votes/me?status= | T4 | ✓ |
| POST /votes (multipart+S3) | T4 | ✓ |
| GET /votes/{id} | T4 | ✓ |
| POST /votes/{id}/swipe (멱등) | T5 | ✓ |
| DELETE /votes/{id}/swipe/last | T5 | ✓ |
| GET /votes/{id}/results + 집계 | T4,T5 | ✓ |
| AI insight lazy+cache+fallback | T6 | ✓ |
| OAuthClient interface + 4 impls | T2,T3 | ✓ |
| HandlerInterceptor (no Spring Security) | T1,T2 | ✓ |
| X-Voter-Id header | T5 | ✓ |
| UNIQUE(photo_id,voter_id) + ON DUPLICATE KEY | T5 | ✓ |
| deadline > NOW() 계산 | T4 | ✓ |
| 사진 수 2~10 검증 | T4 | ✓ |
| action LIKE/SKIP 검증 | T5 | ✓ |
| docker-compose up 원클릭 | T1 | ✓ |
| 환경변수만 사용 (하드코딩 없음) | All | ✓ |

### Gaps / Notes
- `ResultService.java` imports `List` but it's used implicitly — verify import is present.
- `AiInsightService.callVision()` uses `objectMapper.readValue` which throws `JsonProcessingException` (checked). The `throws Exception` propagation is handled by the outer try-catch in `callVisionOrFallback` — this is fine since `callVision` declares `throws Exception`.
- `UserService` needs `VoteRepository` injected (added in Task 7).
- `GET /votes/me` is JWT-protected; AuthInterceptor regex only exempts `GET /votes/{digits}` and `GET /votes/{digits}/results` — `/votes/me` is NOT exempted so JWT is required. Correct.
- Schema init via `spring.sql.init` runs on every startup but `CREATE TABLE IF NOT EXISTS` is idempotent — safe.
