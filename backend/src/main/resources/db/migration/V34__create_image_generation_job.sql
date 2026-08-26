CREATE TABLE image_generation_job (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES user_account (id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL REFERENCES conversation (id) ON DELETE CASCADE,
    prompt VARCHAR(4000) NOT NULL,
    status VARCHAR(24) NOT NULL,
    provider VARCHAR(40) NOT NULL,
    runtime_job_id VARCHAR(160),
    model VARCHAR(255),
    progress INTEGER,
    eta_seconds INTEGER,
    artifact_path VARCHAR(500),
    media_type VARCHAR(100),
    error_code VARCHAR(80),
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_image_generation_status CHECK (
        status IN ('QUEUED', 'GENERATING', 'COMPLETED', 'FAILED', 'CANCELLED')
    ),
    CONSTRAINT ck_image_generation_progress CHECK (
        progress IS NULL OR (progress >= 0 AND progress <= 100)
    )
);

CREATE INDEX ix_image_generation_conversation_created
    ON image_generation_job (conversation_id, created_at DESC);

CREATE INDEX ix_image_generation_user_status
    ON image_generation_job (user_id, status);
