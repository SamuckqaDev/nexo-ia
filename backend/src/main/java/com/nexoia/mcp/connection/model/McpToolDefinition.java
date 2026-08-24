package com.nexoia.mcp.connection.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** A sanitized snapshot of one tool discovered from an MCP server. */
@Getter
@Builder
@Entity
@Table(name = "mcp_tool_definition")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class McpToolDefinition {

    @Id
    private UUID id;

    @Column(name = "connection_id", nullable = false)
    private UUID connectionId;

    @Column(name = "external_name", nullable = false, length = 160)
    private String externalName;

    @Column(name = "exposed_name", nullable = false, unique = true, length = 160)
    private String exposedName;

    @Column(length = 200)
    private String title;

    @Column(length = 2000)
    private String description;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "input_schema", nullable = false, columnDefinition = "jsonb")
    private Map<String, Object> inputSchema;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "read_only_hint")
    private Boolean readOnlyHint;

    @Column(name = "destructive_hint")
    private Boolean destructiveHint;

    @Column(name = "open_world_hint")
    private Boolean openWorldHint;

    @Column(name = "discovered_at", nullable = false)
    private Instant discoveredAt;

    public void setEnabled(boolean value) {
        enabled = value;
    }
}
