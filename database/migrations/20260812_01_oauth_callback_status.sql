ALTER TABLE oauth_authorization_request
    ADD COLUMN status VARCHAR(20) NULL AFTER used_at,
    ADD COLUMN failure_stage VARCHAR(30) NULL AFTER status,
    ADD COLUMN failure_type VARCHAR(30) NULL AFTER failure_stage,
    ADD COLUMN processing_started_at DATETIME(6) NULL AFTER failure_type,
    ADD COLUMN completed_at DATETIME(6) NULL AFTER processing_started_at;

UPDATE oauth_authorization_request
SET status = CASE WHEN used_at IS NULL THEN 'ISSUED' ELSE 'LEGACY' END,
    processing_started_at = used_at,
    completed_at = used_at
WHERE status IS NULL;

ALTER TABLE oauth_authorization_request
    MODIFY COLUMN status VARCHAR(20) NOT NULL;

CREATE INDEX idx_oauth_authorization_request_status_processing
    ON oauth_authorization_request (status, processing_started_at);
