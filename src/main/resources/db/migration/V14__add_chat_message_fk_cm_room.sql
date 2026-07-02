ALTER TABLE chat_message DROP FOREIGN KEY fk_cm_room;
ALTER TABLE chat_message ADD CONSTRAINT fk_cm_room
FOREIGN KEY (chat_room_id) REFERENCES chat_room (id) ON DELETE CASCADE;