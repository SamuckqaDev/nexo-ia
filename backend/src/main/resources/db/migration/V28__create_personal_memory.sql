CREATE TABLE personal_memory (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES user_account (id) ON DELETE CASCADE,
    content VARCHAR(1000) NOT NULL,
    source_conversation_id UUID REFERENCES conversation (id) ON DELETE SET NULL,
    source_message_id UUID REFERENCES conversation_message (id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_personal_memory_user_updated
    ON personal_memory (user_id, updated_at DESC);
