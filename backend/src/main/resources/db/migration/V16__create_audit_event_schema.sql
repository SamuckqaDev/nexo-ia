-- The unified security audit trail. Distinct from access_event, which records session and device
-- activity for the member-facing panel; audit_event records domain lifecycle and administrative
-- actions for an authorized reviewer, correlated across model requests, usage, and messages.
CREATE TABLE audit_event (
    id UUID PRIMARY KEY,
    action VARCHAR(48) NOT NULL,
    outcome VARCHAR(16) NOT NULL,
    actor_user_id UUID REFERENCES user_account (id) ON DELETE SET NULL,
    actor_role VARCHAR(24),
    target_type VARCHAR(32),
    target_id UUID,
    correlation_id UUID,
    detail VARCHAR(256),
    occurred_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_audit_event_outcome CHECK (outcome IN ('SUCCESS', 'FAILURE', 'DENIED'))
);

CREATE INDEX ix_audit_event_occurred ON audit_event (occurred_at DESC);
CREATE INDEX ix_audit_event_actor ON audit_event (actor_user_id, occurred_at DESC);
CREATE INDEX ix_audit_event_correlation ON audit_event (correlation_id);
