-- 1. 질문 테이블
CREATE TABLE question (
    question_id BIGINT AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    content TEXT NOT NULL,
    view_count BIGINT DEFAULT 0,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    category VARCHAR(50),
    deleted_at DATETIME,
    user_id BIGINT NOT NULL,
    language_id BIGINT NOT NULL,
    CONSTRAINT pk_question PRIMARY KEY (question_id),
    CONSTRAINT fk_question_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_question_language FOREIGN KEY (language_id) REFERENCES language (language_id)
);

-- 2. 질문 첨부파일 테이블
CREATE TABLE question_attachment (
    id BIGINT AUTO_INCREMENT,
    original_name VARCHAR(255) NOT NULL,
    stored_name VARCHAR(255) NOT NULL,
    file_path VARCHAR(500) NOT NULL,
    file_type VARCHAR(100),
    file_size BIGINT,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    question_id BIGINT NOT NULL,
    CONSTRAINT pk_question_attachment PRIMARY KEY (id),
    CONSTRAINT fk_attachment_question FOREIGN KEY (question_id) REFERENCES question (question_id)
);

-- 3. 질문 좋아요 테이블
CREATE TABLE question_like (
    id BIGINT AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    CONSTRAINT pk_question_like PRIMARY KEY (id),
    CONSTRAINT fk_ql_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_ql_question FOREIGN KEY (question_id) REFERENCES question (question_id)
);

-- 4. 답변 테이블
CREATE TABLE answer (
    answer_id BIGINT AUTO_INCREMENT,
    post_id BIGINT,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at DATETIME,
    is_accepted TINYINT(1) DEFAULT 0,
    like_count INT DEFAULT 0,
    user_id BIGINT NOT NULL,
    question_id BIGINT NOT NULL,
    CONSTRAINT pk_answer PRIMARY KEY (answer_id),
    CONSTRAINT fk_answer_parent FOREIGN KEY (post_id) REFERENCES answer (answer_id),
    CONSTRAINT fk_answer_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_answer_question FOREIGN KEY (question_id) REFERENCES question (question_id)
);

-- 5. 답변 좋아요 테이블
CREATE TABLE answer_like (
    id BIGINT AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    answer_id BIGINT NOT NULL,
    CONSTRAINT pk_answer_like PRIMARY KEY (id),
    CONSTRAINT fk_al_user FOREIGN KEY (user_id) REFERENCES users (user_id),
    CONSTRAINT fk_al_answer FOREIGN KEY (answer_id) REFERENCES answer (answer_id)
);

-- 6. 댓글 테이블
CREATE TABLE comment (
    comment_id BIGINT AUTO_INCREMENT,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    answer_id BIGINT NOT NULL,
    user_id BIGINT NOT NULL,
    deleted_at DATETIME,
    CONSTRAINT pk_comment PRIMARY KEY (comment_id),
    CONSTRAINT fk_comment_answer FOREIGN KEY (answer_id) REFERENCES answer (answer_id),
    CONSTRAINT fk_comment_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);
