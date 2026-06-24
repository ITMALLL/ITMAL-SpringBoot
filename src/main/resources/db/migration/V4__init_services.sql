-- 1. 알림 테이블
CREATE TABLE notification (
    notification_id BIGINT AUTO_INCREMENT,
    type VARCHAR(30) NOT NULL,
    target_type VARCHAR(30),
    target_id BIGINT,
    is_read BOOLEAN DEFAULT FALSE,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT NOT NULL,
    CONSTRAINT pk_notification PRIMARY KEY (notification_id),
    CONSTRAINT fk_notification_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);

-- 2. 신고 테이블
CREATE TABLE report (
    report_id BIGINT AUTO_INCREMENT,
    target_type VARCHAR(20) NOT NULL,
    target_id BIGINT NOT NULL,
    reason TEXT NOT NULL,
    status VARCHAR(20) NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id BIGINT NOT NULL,
    CONSTRAINT pk_report PRIMARY KEY (report_id),
    CONSTRAINT fk_report_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);
