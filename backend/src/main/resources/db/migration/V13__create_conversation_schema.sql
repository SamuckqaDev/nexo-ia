CREATE TABLE conversation (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES user_account (id) ON DELETE CASCADE,
    title VARCHAR(160) NOT NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE conversation_message (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversation (id) ON DELETE CASCADE,
    sequence_number INTEGER NOT NULL,
    role VARCHAR(16) NOT NULL,
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_conversation_message_role CHECK (role IN ('USER', 'ASSISTANT')),
    CONSTRAINT uk_conversation_message_sequence UNIQUE (conversation_id, sequence_number)
);

CREATE INDEX ix_conversation_user_updated ON conversation (user_id, updated_at DESC);
CREATE INDEX ix_conversation_message_conversation_sequence ON conversation_message (conversation_id, sequence_number);
