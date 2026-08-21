-- A Knowledge Vault owned by a single user. Scope PERSONAL is always valid; scope WORKSPACE requires
-- a workspace_id resolving to a workspace owned by the same user. PROJECT, TEAM, and ORGANIZATION are
-- accepted by the enum contract but rejected by KnowledgeVaultService before a row is ever written,
-- because no backend project/team/organization entity exists yet. See D-026.
CREATE TABLE knowledge_vault (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES user_account (id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    description VARCHAR(500),
    scope VARCHAR(32) NOT NULL,
    workspace_id UUID REFERENCES workspace (id) ON DELETE SET NULL,
    archived BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_knowledge_vault_scope
        CHECK (scope IN ('PERSONAL', 'WORKSPACE', 'PROJECT', 'TEAM', 'ORGANIZATION')),
    CONSTRAINT ck_knowledge_vault_workspace_scope
        CHECK (scope <> 'WORKSPACE' OR workspace_id IS NOT NULL)
);

CREATE INDEX ix_knowledge_vault_owner ON knowledge_vault (owner_id, archived);
CREATE INDEX ix_knowledge_vault_workspace ON knowledge_vault (workspace_id);
