-- A Vault owner is polymorphic from V32 onward: USER resolves to user_account and TEAM resolves to
-- team. The legacy foreign key could only reference users and rejected every Team-owned Vault.
ALTER TABLE knowledge_vault
    DROP CONSTRAINT IF EXISTS knowledge_vault_owner_id_fkey;

ALTER TABLE knowledge_vault
    ADD CONSTRAINT ck_knowledge_vault_owner_type
    CHECK (owner_type IN ('USER', 'TEAM'));

CREATE INDEX ix_knowledge_vault_owner_type
    ON knowledge_vault (owner_type, owner_id, archived);

ALTER TABLE team
    ADD CONSTRAINT fk_team_created_by
    FOREIGN KEY (created_by) REFERENCES user_account (id) ON DELETE RESTRICT;

ALTER TABLE team_membership
    ADD CONSTRAINT fk_team_membership_user
    FOREIGN KEY (user_id) REFERENCES user_account (id) ON DELETE CASCADE;

-- UserRole gained ADMIN in the governance slice. Keep the database constraint aligned with the
-- Java/API contract before an administrator account can be persisted.
ALTER TABLE user_account
    DROP CONSTRAINT ck_user_account_role;

ALTER TABLE user_account
    ADD CONSTRAINT ck_user_account_role
    CHECK (role IN ('OWNER', 'ADMIN', 'MEMBER'));
