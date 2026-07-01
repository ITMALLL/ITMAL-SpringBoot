-- answer_like 유니크 제약 추가,같은 유저가 같은 답변에 좋아요 두 번 못하게 DB에서 막기
ALTER TABLE answer_like ADD CONSTRAINT uq_answer_like
UNIQUE (answer_id, user_id);

-- question 파파고 번역 기능용 컬럼 추가
ALTER TABLE question
    ADD COLUMN translated_content VARCHAR(5000) NULL AFTER content,
    ADD COLUMN translated_target VARCHAR(10) NULL AFTER translated_content;