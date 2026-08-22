CREATE TABLE conversation_knowledge_vault (
    id UUID PRIMARY KEY,
    conversation_id UUID NOT NULL REFERENCES conversation (id) ON DELETE CASCADE,
    vault_id UUID NOT NULL REFERENCES knowledge_vault (id) ON DELETE CASCADE,
    selected_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_conversation_knowledge_vault UNIQUE (conversation_id, vault_id)
);

CREATE INDEX ix_conversation_knowledge_vault_conversation
    ON conversation_knowledge_vault (conversation_id, selected_at);
CREATE INDEX ix_conversation_knowledge_vault_vault
    ON conversation_knowledge_vault (vault_id);
