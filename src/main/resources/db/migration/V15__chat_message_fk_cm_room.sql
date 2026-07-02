ALTER TABLE chat_message
DROP FOREIGN KEY IF EXISTS fk_cm_room,
DROP INDEX IF EXISTS fk_cm_room,
    ADD CONSTRAINT fk_cm_room FOREIGN KEY (chat_room_id) REFERENCES chat_room (id) ON DELETE CASCADE;