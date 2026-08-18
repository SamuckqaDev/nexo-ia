ALTER TABLE conversation_message
    ADD COLUMN status VARCHAR(16) NOT NULL DEFAULT 'COMPLETED',
    ADD COLUMN provider_configuration_id UUID
        REFERENCES provider_configuration (id) ON DELETE SET NULL,
    ADD COLUMN model VARCHAR(160),
    ADD COLUMN input_tokens INTEGER,
    ADD COLUMN output_tokens INTEGER,
    ADD COLUMN token_source VARCHAR(16),
    ADD COLUMN latency_ms BIGINT,
    ADD COLUMN processing_location VARCHAR(16),
    ADD COLUMN failure_code VARCHAR(64),
    ADD COLUMN correlation_id UUID,
    ADD COLUMN completed_at TIMESTAMPTZ;

ALTER TABLE conversation_message
    ADD CONSTRAINT ck_conversation_message_status CHECK (status IN (
        'QUEUED', 'STREAMING', 'CANCELLING', 'COMPLETED', 'CANCELLED', 'FAILED'));

ALTER TABLE conversation_message
    ADD CONSTRAINT ck_conversation_message_token_source CHECK (
        token_source IS NULL OR token_source IN ('PROVIDER', 'ESTIMATE'));

ALTER TABLE conversation_message
    ADD CONSTRAINT ck_conversation_message_processing_location CHECK (
        processing_location IS NULL OR processing_location IN ('LOCAL', 'REMOTE'));

-- Existing rows predate streaming and are already terminal.
ALTER TABLE conversation_message ALTER COLUMN status DROP DEFAULT;

-- One conversation may hold at most one non-terminal model request. The database owns this rule so
-- a concurrent submission fails on insert instead of racing an application-side check.
CREATE UNIQUE INDEX ux_conversation_message_active_request
    ON conversation_message (conversation_id)
    WHERE status IN ('QUEUED', 'STREAMING', 'CANCELLING');
