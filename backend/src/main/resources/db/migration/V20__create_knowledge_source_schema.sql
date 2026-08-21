-- A bounded, ingestible source registered under a Knowledge Vault. normalized_content holds the
-- extracted, size-capped text used for chunking; it stays NULL for an UNSUPPORTED source (metadata
-- only, never chunked or embedded). content_hash is a SHA-256 hex digest of normalized_content, used
-- to make re-ingestion idempotent per vault. Never a path column: display_name is a safe, user-
-- supplied label only. See D-026.
CREATE TABLE knowledge_source (
    id UUID PRIMARY KEY,
    vault_id UUID NOT NULL REFERENCES knowledge_vault (id) ON DELETE CASCADE,
    source_kind VARCHAR(16) NOT NULL,
    display_name VARCHAR(200) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    content_hash CHAR(64) NOT NULL,
    byte_size INTEGER NOT NULL,
    normalized_content TEXT,
    status VARCHAR(16) NOT NULL,
    error_code VARCHAR(64),
    metadata JSONB,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_knowledge_source_vault_hash UNIQUE (vault_id, content_hash),
    CONSTRAINT ck_knowledge_source_kind CHECK (source_kind IN ('UPLOAD')),
    CONSTRAINT ck_knowledge_source_status
        CHECK (status IN ('REGISTERED', 'INGESTING', 'READY', 'FAILED', 'UNSUPPORTED'))
);

CREATE INDEX ix_knowledge_source_vault ON knowledge_source (vault_id, archived);
CREATE INDEX ix_knowledge_source_status ON knowledge_source (status);
