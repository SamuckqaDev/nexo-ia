package com.nexoia.mcp.connection.repository;

import com.nexoia.mcp.connection.model.McpToolDefinition;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface McpToolDefinitionRepository extends JpaRepository<McpToolDefinition, UUID> {

    List<McpToolDefinition> findAllByConnectionIdOrderByExternalNameAsc(UUID connectionId);

    List<McpToolDefinition> findAllByConnectionIdInAndEnabledTrueOrderByExposedNameAsc(
            Collection<UUID> connectionIds);

    void deleteAllByConnectionId(UUID connectionId);
}
