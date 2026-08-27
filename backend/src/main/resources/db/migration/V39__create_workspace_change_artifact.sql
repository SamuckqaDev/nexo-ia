CREATE TABLE workspace_change_artifact (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES user_account (id) ON DELETE CASCADE,
    conversation_id UUID NOT NULL REFERENCES conversation (id) ON DELETE CASCADE,
    assistant_message_id UUID NOT NULL REFERENCES conversation_message (id) ON DELETE CASCADE,
    correlation_id UUID NOT NULL,
    workspace_id UUID NOT NULL REFERENCES workspace (id) ON DELETE CASCADE,
    workspace_binding_id UUID REFERENCES workspace_binding (id) ON DELETE SET NULL,
    operation VARCHAR(24) NOT NULL,
    status VARCHAR(32) NOT NULL,
    relative_path VARCHAR(1024) NOT NULL,
    before_sha256 VARCHAR(64),
    after_sha256 VARCHAR(64),
    before_artifact_key VARCHAR(500),
    after_artifact_key VARCHAR(500),
    replacement_count INTEGER,
    failure_code VARCHAR(80),
    decided_at TIMESTAMPTZ,
    applied_at TIMESTAMPTZ,
    reverted_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT ck_workspace_change_operation CHECK (operation IN ('CREATE', 'EDIT', 'DELETE')),
    CONSTRAINT ck_workspace_change_status CHECK (
        status IN ('PENDING_APPROVAL', 'APPLIED', 'DENIED', 'INVALIDATED', 'REVERTED', 'FAILED')
    )
);

CREATE INDEX ix_workspace_change_conversation_created
    ON workspace_change_artifact (conversation_id, created_at DESC);

CREATE INDEX ix_workspace_change_user_status
    ON workspace_change_artifact (user_id, status, created_at DESC);
