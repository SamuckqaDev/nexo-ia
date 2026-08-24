CREATE TABLE agent_plan (
    id UUID PRIMARY KEY,
    assistant_message_id UUID NOT NULL UNIQUE REFERENCES conversation_message (id) ON DELETE CASCADE,
    revision INTEGER NOT NULL,
    explanation VARCHAR(500),
    steps JSONB NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
