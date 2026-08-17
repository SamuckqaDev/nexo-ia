CREATE TABLE login_attempt (
    id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    identifier_hash VARCHAR(64) NOT NULL,
    ip_address VARCHAR(45) NOT NULL,
    successful BOOLEAN NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL
);

CREATE INDEX idx_login_attempt_identity_ip_time
    ON login_attempt (identifier_hash, ip_address, occurred_at DESC);
