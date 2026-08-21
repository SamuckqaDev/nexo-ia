-- Citations for an assistant message that used Knowledge Vault retrieval. Populated only on the
-- assistant message; the retrieved excerpts themselves are never persisted, only these bounded
-- citations (vault/source names, chunk ordinal, excerpt, score). See D-026.
ALTER TABLE conversation_message ADD COLUMN citations JSONB;
