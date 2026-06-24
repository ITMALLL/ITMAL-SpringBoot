-- 1. 언어 마스터 테이블
CREATE TABLE language (
    language_id BIGINT AUTO_INCREMENT,
    language_name VARCHAR(20) NOT NULL,
    CONSTRAINT pk_language PRIMARY KEY (language_id)
);

-- 2. 유저 테이블
CREATE TABLE users (
    user_id BIGINT AUTO_INCREMENT,
    email VARCHAR(100) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    nickname VARCHAR(50) NOT NULL UNIQUE,
    role ENUM('ROLE_USER', 'ROLE_TUTOR', 'ROLE_MODERATOR', 'ROLE_ADMIN') NOT NULL,
    native_language VARCHAR(20),
    provider VARCHAR(20),
    provider_id VARCHAR(100),
    email_verified TINYINT(1) DEFAULT 0,
    deleted_at DATETIME,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (user_id)
);

-- 3. 유저 학습 언어 매핑 테이블
CREATE TABLE user_learning_language (
    id BIGINT AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    language_id BIGINT NOT NULL,
    CONSTRAINT pk_user_learning_language PRIMARY KEY (id),
    CONSTRAINT uq_ull_user_language UNIQUE (user_id, language_id),
    CONSTRAINT fk_ull_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_ull_language FOREIGN KEY (language_id) REFERENCES language (language_id)
);
