DROP PROCEDURE IF EXISTS fix_fk_cm_room;

CREATE PROCEDURE fix_fk_cm_room()
BEGIN
    IF EXISTS (
        SELECT 1 FROM information_schema.TABLE_CONSTRAINTS
        WHERE CONSTRAINT_SCHEMA = DATABASE()
          AND TABLE_NAME = 'chat_message'
          AND CONSTRAINT_NAME = 'fk_cm_room'
          AND CONSTRAINT_TYPE = 'FOREIGN KEY'
    ) THEN
ALTER TABLE chat_message DROP FOREIGN KEY fk_cm_room;
END IF;
    IF EXISTS (
        SELECT 1 FROM information_schema.STATISTICS
        WHERE TABLE_SCHEMA = DATABASE()
          AND TABLE_NAME = 'chat_message'
          AND INDEX_NAME = 'fk_cm_room'
    ) THEN
ALTER TABLE chat_message DROP INDEX fk_cm_room;
END IF;
ALTER TABLE chat_message ADD CONSTRAINT fk_cm_room FOREIGN KEY (chat_room_id) REFERENCES chat_room (id) ON DELETE CASCADE;
END;

CALL fix_fk_cm_room();
DROP PROCEDURE IF EXISTS fix_fk_cm_room;