ALTER TABLE language ADD COLUMN language_code VARCHAR(10) NULL AFTER language_name;

UPDATE language SET language_code = 'en' WHERE language_name = '영어';
UPDATE language SET language_code = 'ja' WHERE language_name = '일본어';
UPDATE language SET language_code = 'zh-CN' WHERE language_name = '중국어';
UPDATE language SET language_code = 'fr' WHERE language_name = '프랑스어';
UPDATE language SET language_code = 'es' WHERE language_name = '스페인어';
UPDATE language SET language_code = 'de' WHERE language_name = '독일어';
UPDATE language SET language_code = 'ko' WHERE language_name = '한국어';

ALTER TABLE language MODIFY COLUMN language_code VARCHAR(10) NOT NULL;