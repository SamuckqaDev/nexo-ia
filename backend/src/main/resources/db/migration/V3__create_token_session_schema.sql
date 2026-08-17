CREATE TABLE auth_session (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES user_account (id) ON DELETE CASCADE,
    status VARCHAR(24) NOT NULL,
    current_access_jti UUID NOT NULL,
    initial_ip VARCHAR(45) NOT NULL,
    last_ip VARCHAR(45) NOT NULL,
    user_agent VARCHAR(512) NOT NULL,
    access_expires_at TIMESTAMPTZ NOT NULL,
    refresh_expires_at TIMESTAMPTZ NOT NULL,
    last_seen_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    revoked_at TIMESTAMPTZ,
    revoke_reason VARCHAR(120),
    CONSTRAINT ck_auth_session_status CHECK (status IN ('ACTIVE', 'REVOKED', 'COMPROMISED'))
);

CREATE INDEX idx_auth_session_user_status ON auth_session (user_id, status);
CREATE UNIQUE INDEX uk_auth_session_access_jti ON auth_session (current_access_jti);

CREATE TABLE refresh_token (
    id UUID PRIMARY KEY,
    session_id UUID NOT NULL REFERENCES auth_session (id) ON DELETE CASCADE,
    token_hash VARCHAR(64) NOT NULL,
    issued_ip VARCHAR(45) NOT NULL,
    issued_at TIMESTAMPTZ NOT NULL,
    expires_at TIMESTAMPTZ NOT NULL,
    used_at TIMESTAMPTZ,
    replaced_by UUID REFERENCES refresh_token (id),
    CONSTRAINT uk_refresh_token_hash UNIQUE (token_hash)
);

CREATE INDEX idx_refresh_token_session ON refresh_token (session_id);

CREATE TABLE access_event (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    session_id UUID REFERENCES auth_session (id) ON DELETE SET NULL,
    user_id UUID REFERENCES user_account (id) ON DELETE SET NULL,
    event_type VARCHAR(40) NOT NULL,
    success BOOLEAN NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    user_agent VARCHAR(512) NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_access_event_user_time ON access_event (user_id, occurred_at DESC);
CREATE INDEX idx_access_event_session_time ON access_event (session_id, occurred_at DESC);
