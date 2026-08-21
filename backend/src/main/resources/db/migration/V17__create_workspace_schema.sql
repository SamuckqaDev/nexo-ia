-- A minimal owned-name Workspace, introduced so Knowledge Vault scope "workspace" has a real,
-- authorized backend target. Deliberately just an owner and a name: the full project/workspace
-- product surface (local directory handles, snapshots) stays a client-local IndexedDB concept in the
-- frontend and is not unified with this record in this release. See D-026.
CREATE TABLE workspace (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES user_account (id) ON DELETE CASCADE,
    name VARCHAR(160) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_workspace_owner ON workspace (owner_id, created_at DESC);
