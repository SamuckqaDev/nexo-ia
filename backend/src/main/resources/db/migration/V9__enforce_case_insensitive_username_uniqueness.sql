CREATE UNIQUE INDEX uk_user_account_username_case_insensitive
    ON user_account (LOWER(username));
