-- 1. 채팅 신청 테이블
CREATE TABLE chat_request (
    chat_request_id BIGINT AUTO_INCREMENT,
    requester_id BIGINT NOT NULL,
    responder_id BIGINT NOT NULL,
    intro_message VARCHAR(255),
    status ENUM('PENDING', 'ACCEPTED', 'REJECTED') NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    responded_at DATETIME,
    CONSTRAINT pk_chat_request PRIMARY KEY (chat_request_id),
    CONSTRAINT fk_cr_requester FOREIGN KEY (requester_id) REFERENCES users (user_id),
    CONSTRAINT fk_cr_responder FOREIGN KEY (responder_id) REFERENCES users (user_id)
);

-- 2. 채팅방 테이블
CREATE TABLE chat_room (
    id BIGINT AUTO_INCREMENT,
    chat_request_id BIGINT NOT NULL,
    chat_title VARCHAR(255),
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    last_message_at DATETIME,
    last_read_at_a DATETIME,
    last_read_at_b DATETIME,
    hidden_by_a BOOLEAN DEFAULT FALSE,
    hidden_by_b BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_chat_room PRIMARY KEY (id),
    CONSTRAINT fk_chatroom_request FOREIGN KEY (chat_request_id) REFERENCES chat_request (chat_request_id)
);

-- 3. 채팅 메시지 테이블
CREATE TABLE chat_message (
    chat_message_id BIGINT AUTO_INCREMENT,
    chat_room_id BIGINT NOT NULL,
    sender_id BIGINT NOT NULL,
    content TEXT NOT NULL,
    created_at DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_chat_message PRIMARY KEY (chat_message_id),
    CONSTRAINT fk_cm_room FOREIGN KEY (chat_room_id) REFERENCES chat_room (id),
    CONSTRAINT fk_cm_sender FOREIGN KEY (sender_id) REFERENCES users (user_id)
);
