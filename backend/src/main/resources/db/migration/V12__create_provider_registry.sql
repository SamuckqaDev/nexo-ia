CREATE TABLE provider_configuration (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES user_account (id) ON DELETE CASCADE,
    provider_type VARCHAR(32) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    endpoint VARCHAR(500) NOT NULL,
    selected_model VARCHAR(160),
    enabled BOOLEAN NOT NULL DEFAULT TRUE,
    last_connected_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_provider_type CHECK (provider_type IN ('OLLAMA', 'OPENAI', 'GOOGLE_GEMINI', 'ANTHROPIC', 'OPENAI_COMPATIBLE')),
    CONSTRAINT uk_provider_user_endpoint UNIQUE (user_id, endpoint)
);

CREATE INDEX ix_provider_configuration_user ON provider_configuration (user_id);
