-- chat_message: is_read 컬럼 추가
ALTER TABLE chat_message ADD COLUMN is_read BOOLEAN DEFAULT FALSE;

-- chat_request 인덱스
CREATE INDEX idx_responder_status ON chat_request(responder_id, status);
CREATE INDEX idx_requester_status ON chat_request(requester_id, status);

-- chat_message 인덱스
CREATE INDEX idx_chat_room_created ON chat_message(chat_room_id, created_at);

-- chat_room 인덱스 및 퇴장 시각 컬럼 추가
CREATE INDEX idx_last_message ON chat_room(last_message_at);
ALTER TABLE chat_room ADD COLUMN left_at_a DATETIME;
ALTER TABLE chat_room ADD COLUMN left_at_b DATETIME;
