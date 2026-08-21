-- A chunk of extracted source text with its embedding vector. Chunks are immutable: re-ingestion of a
-- changed source deletes and recreates its chunks rather than updating them in place. Dimensions are
-- fixed at 768 for the default nomic-embed-text model (D-026); changing the embedding model requires a
-- new migration altering this column plus full re-ingestion. No ANN index yet — plain exact search
-- until corpus size and benchmarks justify one (D-004).
CREATE TABLE knowledge_chunk (
    id UUID PRIMARY KEY,
    source_id UUID NOT NULL REFERENCES knowledge_source (id) ON DELETE CASCADE,
    ordinal INTEGER NOT NULL,
    content TEXT NOT NULL,
    token_estimate INTEGER NOT NULL,
    embedding vector(768) NOT NULL,
    embedding_model VARCHAR(120) NOT NULL,
    embedding_dimensions INTEGER NOT NULL,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_knowledge_chunk_source_ordinal UNIQUE (source_id, ordinal)
);

CREATE INDEX ix_knowledge_chunk_source ON knowledge_chunk (source_id);
