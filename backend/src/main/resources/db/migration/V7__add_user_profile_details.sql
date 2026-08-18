ALTER TABLE user_account
    ADD COLUMN nickname VARCHAR(80),
    ADD COLUMN age SMALLINT;

ALTER TABLE user_account
    ADD CONSTRAINT ck_user_account_age CHECK (age IS NULL OR age BETWEEN 1 AND 120);
