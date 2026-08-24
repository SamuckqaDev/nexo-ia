CREATE TABLE mcp_connection (
    id UUID PRIMARY KEY,
    user_id UUID NOT NULL REFERENCES user_account (id) ON DELETE CASCADE,
    display_name VARCHAR(100) NOT NULL,
    connection_kind VARCHAR(32) NOT NULL,
    transport_type VARCHAR(32) NOT NULL,
    catalog_server_id VARCHAR(120),
    endpoint VARCHAR(500),
    cost_type VARCHAR(32) NOT NULL,
    status VARCHAR(24) NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    server_name VARCHAR(160),
    server_version VARCHAR(80),
    last_error_code VARCHAR(64),
    last_connected_at TIMESTAMPTZ,
    created_at TIMESTAMPTZ NOT NULL,
    updated_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ck_mcp_connection_target CHECK (
        (connection_kind = 'DOCKER_CATALOG' AND catalog_server_id IS NOT NULL AND endpoint IS NULL)
        OR
        (connection_kind = 'CUSTOM_REMOTE' AND endpoint IS NOT NULL AND catalog_server_id IS NULL)
    )
);

CREATE UNIQUE INDEX ux_mcp_connection_user_name
    ON mcp_connection (user_id, LOWER(display_name));

CREATE INDEX ix_mcp_connection_user_enabled
    ON mcp_connection (user_id, enabled);

CREATE TABLE mcp_tool_definition (
    id UUID PRIMARY KEY,
    connection_id UUID NOT NULL REFERENCES mcp_connection (id) ON DELETE CASCADE,
    external_name VARCHAR(160) NOT NULL,
    exposed_name VARCHAR(160) NOT NULL,
    title VARCHAR(200),
    description VARCHAR(2000),
    input_schema JSONB NOT NULL,
    enabled BOOLEAN NOT NULL DEFAULT FALSE,
    read_only_hint BOOLEAN,
    destructive_hint BOOLEAN,
    open_world_hint BOOLEAN,
    discovered_at TIMESTAMPTZ NOT NULL,
    CONSTRAINT ux_mcp_tool_connection_external UNIQUE (connection_id, external_name),
    CONSTRAINT ux_mcp_tool_exposed_name UNIQUE (exposed_name)
);

CREATE INDEX ix_mcp_tool_connection_enabled
    ON mcp_tool_definition (connection_id, enabled);
