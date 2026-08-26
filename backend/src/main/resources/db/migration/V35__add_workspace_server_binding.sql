-- Expand the minimal Workspace record into a server-managed, bindable project. Legacy rows keep
-- their UNBOUND/READ_ONLY defaults so they remain valid Knowledge Vault scope targets. See D-032.

ALTER TABLE workspace
    ADD COLUMN storage_type VARCHAR(32) NOT NULL DEFAULT 'UNBOUND',
    ADD COLUMN access_mode VARCHAR(40) NOT NULL DEFAULT 'READ_ONLY',
    ADD COLUMN relative_path VARCHAR(1024),
    ADD COLUMN structure_fingerprint VARCHAR(64),
    ADD COLUMN git_head VARCHAR(64),
    ADD COLUMN last_scanned_at TIMESTAMPTZ;

ALTER TABLE workspace
    ADD CONSTRAINT ck_workspace_storage_type CHECK (
        storage_type IN ('UNBOUND', 'MANAGED', 'MOUNTED')
    ),
    ADD CONSTRAINT ck_workspace_access_mode CHECK (
        access_mode IN ('READ_ONLY', 'WRITE_WITH_APPROVAL', 'COMMANDS_WITH_APPROVAL')
    ),
    ADD CONSTRAINT ck_workspace_mounted_requires_path CHECK (
        storage_type <> 'MOUNTED' OR relative_path IS NOT NULL
    );

CREATE INDEX ix_workspace_owner_storage
    ON workspace (owner_id, storage_type);

ALTER TABLE conversation
    ADD COLUMN workspace_id UUID REFERENCES workspace (id) ON DELETE SET NULL;

CREATE INDEX ix_conversation_workspace
    ON conversation (workspace_id);
