ALTER TABLE diary_context
    ADD COLUMN context_key INT NOT NULL DEFAULT 0 AFTER context_type;

ALTER TABLE diary_context
    DROP INDEX uk_diary_context_diary_type,
    ADD CONSTRAINT uk_diary_context_diary_type_key
        UNIQUE (diary_id, context_type, context_key);
