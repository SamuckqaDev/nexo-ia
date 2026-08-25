-- A Vault is read-only knowledge by default. The owner explicitly marks a Vault writable before the
-- assistant's governed save_to_vault tool may append AGENT-authored knowledge to it.
ALTER TABLE knowledge_vault
    ADD COLUMN writable BOOLEAN NOT NULL DEFAULT FALSE;
