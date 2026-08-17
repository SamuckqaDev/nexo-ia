CREATE TABLE profile_avatar (
    user_id UUID PRIMARY KEY REFERENCES user_account (id) ON DELETE CASCADE,
    content_type VARCHAR(32) NOT NULL,
    content BYTEA NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);
