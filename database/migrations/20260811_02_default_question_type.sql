ALTER TABLE default_question
    ADD COLUMN question_code VARCHAR(100) NULL
        AFTER default_question_id;

UPDATE default_question
SET question_code = CONCAT('LEGACY_MAIN_', LPAD(default_question_id, 10, '0'))
WHERE question_code IS NULL;

ALTER TABLE default_question
    MODIFY COLUMN question_code VARCHAR(100) NOT NULL,
    ADD CONSTRAINT uk_default_question_code UNIQUE (question_code),
    ADD COLUMN question_type VARCHAR(20) NOT NULL DEFAULT 'MAIN'
        AFTER interest_tag_id,
    MODIFY COLUMN interest_tag_id BIGINT NULL;

ALTER TABLE default_question
    ADD CONSTRAINT chk_default_question_type_interest
        CHECK (
            question_type IN ('MAIN', 'FOLLOW_UP')
            AND (question_type <> 'MAIN' OR interest_tag_id IS NOT NULL)
        );
