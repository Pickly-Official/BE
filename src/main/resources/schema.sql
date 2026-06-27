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
