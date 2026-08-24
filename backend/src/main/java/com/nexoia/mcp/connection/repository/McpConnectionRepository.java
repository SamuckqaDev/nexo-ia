package com.nexoia.mcp.connection.repository;

import com.nexoia.mcp.connection.model.McpConnection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface McpConnectionRepository extends JpaRepository<McpConnection, UUID> {

    List<McpConnection> findAllByUserIdOrderByCreatedAtAsc(UUID userId);

    List<McpConnection> findAllByUserIdAndEnabledTrueOrderByCreatedAtAsc(UUID userId);

    Optional<McpConnection> findByIdAndUserId(UUID id, UUID userId);

    boolean existsByUserIdAndDisplayNameIgnoreCase(UUID userId, String displayName);

    boolean existsByUserIdAndCatalogServerId(UUID userId, String catalogServerId);

    long countByUserIdAndEnabledTrue(UUID userId);
}
