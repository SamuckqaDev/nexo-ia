-- The capability profile assigned to a user, bounded by the authority of whoever created them.
-- Backfill preserves current behavior: members keep the researcher-level profile the agent path used,
-- and the owner gets the operator profile (unbounded root).
ALTER TABLE user_account
    ADD COLUMN assigned_profile VARCHAR(24) NOT NULL DEFAULT 'RESEARCHER';

UPDATE user_account SET assigned_profile = 'OPERATOR' WHERE role = 'OWNER';
