ALTER TABLE question
    ADD COLUMN translated_content VARCHAR(5000) NULL AFTER content,
    ADD COLUMN translated_target  VARCHAR(10)   NULL AFTER translated_content;
