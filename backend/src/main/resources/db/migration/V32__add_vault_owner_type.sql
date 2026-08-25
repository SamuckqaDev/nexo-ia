-- A Knowledge Vault is owned by a USER (private) or a TEAM (shared with that team's members). The
-- owner_id column already exists; owner_type disambiguates what it points at. Existing Vaults are USER.
ALTER TABLE knowledge_vault
    ADD COLUMN owner_type VARCHAR(16) NOT NULL DEFAULT 'USER';
