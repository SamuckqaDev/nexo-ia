ALTER TABLE conversation_message
    ADD COLUMN conversation_mode VARCHAR(16) NOT NULL DEFAULT 'CHAT',
    ADD COLUMN agent_state VARCHAR(24);

CREATE TABLE tool_execution (
    id UUID PRIMARY KEY,
    assistant_message_id UUID NOT NULL REFERENCES conversation_message (id) ON DELETE CASCADE,
    correlation_id UUID NOT NULL,
    tool_name VARCHAR(80) NOT NULL,
    arguments_digest VARCHAR(64) NOT NULL,
    status VARCHAR(24) NOT NULL,
    duration_ms BIGINT,
    citations JSONB,
    started_at TIMESTAMPTZ NOT NULL,
    completed_at TIMESTAMPTZ
);

CREATE INDEX ix_tool_execution_message_started
    ON tool_execution (assistant_message_id, started_at);
