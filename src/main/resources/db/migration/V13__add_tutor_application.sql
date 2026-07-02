CREATE TABLE tutor_application (
    tutor_application_id BIGINT AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    status ENUM('PENDING', 'APPROVED', 'REJECTED') NOT NULL DEFAULT 'PENDING',
    applied_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    processed_at DATETIME,
    CONSTRAINT pk_tutor_application PRIMARY KEY (tutor_application_id),
    CONSTRAINT fk_tutor_application_user FOREIGN KEY (user_id) REFERENCES users (user_id)
);
