-- A WORKSPACE-scoped Vault requires workspace_id to remain non-null. SET NULL would violate that
-- invariant during deletion, so make the dependency explicit and return a controlled conflict.
ALTER TABLE knowledge_vault
    DROP CONSTRAINT IF EXISTS knowledge_vault_workspace_id_fkey;

ALTER TABLE knowledge_vault
    ADD CONSTRAINT fk_knowledge_vault_workspace
        FOREIGN KEY (workspace_id) REFERENCES workspace (id) ON DELETE RESTRICT;
