package com.nexoia.mcp.connection.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;
import org.hibernate.annotations.UpdateTimestamp;

/** One MCP server registration owned by exactly one authenticated user. */
@Getter
@Builder
@Entity
@Table(name = "mcp_connection")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class McpConnection {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "display_name", nullable = false, length = 100)
    private String displayName;

    @Enumerated(EnumType.STRING)
    @Column(name = "connection_kind", nullable = false, length = 32)
    private McpConnectionKind connectionKind;

    @Enumerated(EnumType.STRING)
    @Column(name = "transport_type", nullable = false, length = 32)
    private McpTransportType transportType;

    @Column(name = "catalog_server_id", length = 120)
    private String catalogServerId;

    @Column(length = 500)
    private String endpoint;

    @Enumerated(EnumType.STRING)
    @Column(name = "cost_type", nullable = false, length = 32)
    private McpCostType costType;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private McpConnectionStatus status;

    @Column(nullable = false)
    private boolean enabled;

    @Column(name = "server_name", length = 160)
    private String serverName;

    @Column(name = "server_version", length = 80)
    private String serverVersion;

    @Column(name = "last_error_code", length = 64)
    private String lastErrorCode;

    @Column(name = "last_connected_at")
    private Instant lastConnectedAt;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void markConnected(String name, String version, Instant connectedAt) {
        serverName = name;
        serverVersion = version;
        lastConnectedAt = connectedAt;
        lastErrorCode = null;
        status = McpConnectionStatus.CONNECTED;
    }

    public void markUnavailable(String errorCode) {
        lastErrorCode = errorCode;
        status = McpConnectionStatus.UNAVAILABLE;
        enabled = false;
    }

    public void setEnabled(boolean value) {
        enabled = value;
        status = value ? McpConnectionStatus.CONNECTED : McpConnectionStatus.DISABLED;
    }
}
