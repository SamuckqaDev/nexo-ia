package com.nexoia.provider.repository;

import com.nexoia.provider.model.ProviderConfiguration;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderConfigurationRepository extends JpaRepository<ProviderConfiguration, UUID> {
    List<ProviderConfiguration> findAllByUserIdOrderByCreatedAtAsc(UUID userId);
    Optional<ProviderConfiguration> findByIdAndUserId(UUID id, UUID userId);
    boolean existsByUserIdAndEndpoint(UUID userId, String endpoint);
    boolean existsByUserIdAndEndpointAndIdNot(UUID userId, String endpoint, UUID id);
}
