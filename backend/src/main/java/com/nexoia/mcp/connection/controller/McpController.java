package com.nexoia.mcp.connection.controller;

import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.mcp.catalog.dto.McpCatalogResponse;
import com.nexoia.mcp.catalog.service.McpCatalogService;
import com.nexoia.mcp.connection.dto.CreateDockerMcpConnectionRequest;
import com.nexoia.mcp.connection.dto.CreateRemoteMcpConnectionRequest;
import com.nexoia.mcp.connection.dto.McpConnectionResponse;
import com.nexoia.mcp.connection.dto.UpdateMcpConnectionStateRequest;
import com.nexoia.mcp.connection.dto.UpdateMcpToolsRequest;
import com.nexoia.mcp.connection.service.McpConnectionService;
import com.nexoia.mcp.connection.service.McpDiscoveryService;
import com.nexoia.shared.api.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/mcp")
public class McpController {

    private final McpCatalogService catalog;
    private final McpConnectionService connections;
    private final McpDiscoveryService discovery;

    @GetMapping("/catalog")
    @Operation(summary = "Browse Docker's MCP Catalog with free-first metadata")
    public ResponseEntity<BaseResponse<McpCatalogResponse>> catalog() {
        return ResponseEntity.ok(BaseResponse.success(200, "MCP catalog retrieved", catalog.catalog()));
    }

    @GetMapping("/connections")
    @Operation(summary = "List MCP connections owned by the authenticated user")
    public ResponseEntity<BaseResponse<McpConnectionResponse>> list(
            @AuthenticationPrincipal NexoUserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(
                200, "MCP connections retrieved", connections.list(principal.userId())));
    }

    @PostMapping("/connections/docker")
    @Operation(summary = "Install a no-secret server from Docker's MCP Catalog")
    public ResponseEntity<BaseResponse<McpConnectionResponse>> installDocker(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @Valid @RequestBody CreateDockerMcpConnectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                201, "Docker MCP server registered",
                connections.installDocker(principal.userId(), request.catalogServerId())));
    }

    @PostMapping("/connections/remote")
    @Operation(summary = "Register a private Streamable HTTP MCP server")
    public ResponseEntity<BaseResponse<McpConnectionResponse>> createRemote(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @Valid @RequestBody CreateRemoteMcpConnectionRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                201, "Remote MCP server registered",
                connections.createRemote(principal.userId(), request)));
    }

    @PostMapping("/connections/{connectionId}/discover")
    @Operation(summary = "Connect to an owned MCP server and replace its discovered tool snapshot")
    public ResponseEntity<BaseResponse<McpConnectionResponse>> discover(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID connectionId) {
        return ResponseEntity.ok(BaseResponse.success(
                200, "MCP tools discovered", discovery.discover(principal.userId(), connectionId)));
    }

    @PutMapping("/connections/{connectionId}/tools")
    @Operation(summary = "Replace the MCP tools explicitly enabled by the authenticated owner")
    public ResponseEntity<BaseResponse<McpConnectionResponse>> selectTools(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID connectionId,
            @Valid @RequestBody UpdateMcpToolsRequest request) {
        return ResponseEntity.ok(BaseResponse.success(
                200, "MCP tool selection updated",
                connections.selectTools(principal.userId(), connectionId, request)));
    }

    @PutMapping("/connections/{connectionId}/state")
    @Operation(summary = "Enable or disable an owned MCP connection for Agent requests")
    public ResponseEntity<BaseResponse<McpConnectionResponse>> setEnabled(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID connectionId,
            @Valid @RequestBody UpdateMcpConnectionStateRequest request) {
        return ResponseEntity.ok(BaseResponse.success(
                200, "MCP connection state updated",
                connections.setEnabled(principal.userId(), connectionId, request.enabled())));
    }

    @DeleteMapping("/connections/{connectionId}")
    @Operation(summary = "Remove an MCP connection owned by the authenticated user")
    public ResponseEntity<BaseResponse<Void>> remove(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID connectionId) {
        connections.remove(principal.userId(), connectionId);
        return ResponseEntity.ok(BaseResponse.success(200, "MCP connection removed", List.of()));
    }
}
