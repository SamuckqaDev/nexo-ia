CREATE TABLE device_agent (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES user_account (id) ON DELETE CASCADE,
    display_name VARCHAR(160) NOT NULL,
    platform VARCHAR(32) NOT NULL,
    architecture VARCHAR(32) NOT NULL,
    app_version VARCHAR(40) NOT NULL,
    credential_hash VARCHAR(64) NOT NULL UNIQUE,
    status VARCHAR(24) NOT NULL,
    capabilities JSONB NOT NULL DEFAULT '[]'::jsonb,
    last_seen_at TIMESTAMPTZ,
    revoked_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_device_agent_status CHECK (status IN ('OFFLINE', 'ONLINE', 'REVOKED'))
);

CREATE INDEX ix_device_agent_owner_created
    ON device_agent (owner_id, created_at DESC);

CREATE TABLE device_pairing (
    id UUID PRIMARY KEY,
    owner_id UUID NOT NULL REFERENCES user_account (id) ON DELETE CASCADE,
    code_hash VARCHAR(64) NOT NULL UNIQUE,
    expires_at TIMESTAMPTZ NOT NULL,
    consumed_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX ix_device_pairing_owner_created
    ON device_pairing (owner_id, created_at DESC);

CREATE TABLE workspace_binding (
    id UUID PRIMARY KEY,
    workspace_id UUID NOT NULL REFERENCES workspace (id) ON DELETE CASCADE,
    device_id UUID NOT NULL REFERENCES device_agent (id) ON DELETE CASCADE,
    local_binding_id VARCHAR(120) NOT NULL,
    display_name VARCHAR(240) NOT NULL,
    status VARCHAR(24) NOT NULL,
    structure_fingerprint VARCHAR(64),
    git_head VARCHAR(64),
    git_branch VARCHAR(240),
    last_seen_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uq_workspace_binding_device_local UNIQUE (device_id, local_binding_id),
    CONSTRAINT ck_workspace_binding_status CHECK (
        status IN ('AVAILABLE', 'CHANGED', 'OFFLINE', 'MISSING', 'ERROR')
    )
);

CREATE INDEX ix_workspace_binding_workspace
    ON workspace_binding (workspace_id, created_at DESC);

CREATE INDEX ix_workspace_binding_device
    ON workspace_binding (device_id, status);

ALTER TABLE conversation
    ADD COLUMN workspace_binding_id UUID REFERENCES workspace_binding (id) ON DELETE SET NULL;

CREATE INDEX ix_conversation_workspace_binding
    ON conversation (workspace_binding_id);
