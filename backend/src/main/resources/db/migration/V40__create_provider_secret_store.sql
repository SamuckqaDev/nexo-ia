CREATE TABLE provider_secret (
    provider_configuration_id UUID PRIMARY KEY
        REFERENCES provider_configuration (id) ON DELETE CASCADE,
    encrypted_value TEXT NOT NULL,
    initialization_vector VARCHAR(32) NOT NULL,
    key_version INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL
);

COMMENT ON TABLE provider_secret IS
    'Encrypted provider credentials. Plaintext values never leave the backend runtime.';
