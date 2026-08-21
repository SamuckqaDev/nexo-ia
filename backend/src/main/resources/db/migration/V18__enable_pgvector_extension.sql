-- Required by knowledge_chunk.embedding (V21). The local Postgres image was switched to
-- pgvector/pgvector:0.8.6-pg18-bookworm alongside this migration; see D-026.
CREATE EXTENSION IF NOT EXISTS vector;
