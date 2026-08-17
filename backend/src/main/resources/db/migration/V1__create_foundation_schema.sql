CREATE TABLE nexo_installation (
    id UUID PRIMARY KEY,
    installation_name VARCHAR(120) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);
