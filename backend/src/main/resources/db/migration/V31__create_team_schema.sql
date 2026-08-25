-- A Team is the organization/tenant boundary of the governance model: it owns members, shared
-- knowledge, a token budget, media and artifacts, isolated from every other Team.
CREATE TABLE team (
    id UUID PRIMARY KEY,
    name VARCHAR(120) NOT NULL,
    created_by UUID NOT NULL,
    token_budget_limit BIGINT,
    default_profile VARCHAR(24) NOT NULL DEFAULT 'RESEARCHER',
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

CREATE TABLE team_membership (
    id UUID PRIMARY KEY,
    team_id UUID NOT NULL REFERENCES team (id) ON DELETE CASCADE,
    user_id UUID NOT NULL,
    team_role VARCHAR(24) NOT NULL,
    assigned_profile VARCHAR(24) NOT NULL,
    joined_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT uk_team_membership UNIQUE (team_id, user_id)
);

CREATE INDEX ix_team_membership_user ON team_membership (user_id);
CREATE INDEX ix_team_membership_team ON team_membership (team_id);
