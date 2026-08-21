-- V20 created content_hash as CHAR(64), but the KnowledgeSource entity maps it as a length-64 string,
-- which Hibernate validates as VARCHAR(64). Under ddl-auto: validate the CHAR/VARCHAR mismatch fails
-- startup. CHAR(64) also space-pads, which is wrong for a fixed-width hex digest. Align the column to
-- VARCHAR(64) to match the entity; the stored 64-character hashes are unaffected.
ALTER TABLE knowledge_source ALTER COLUMN content_hash TYPE VARCHAR(64);
