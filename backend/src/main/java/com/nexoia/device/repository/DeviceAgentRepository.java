package com.nexoia.device.repository;

import com.nexoia.device.model.DeviceAgent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DeviceAgentRepository extends JpaRepository<DeviceAgent, UUID> {

    List<DeviceAgent> findAllByOwnerIdOrderByCreatedAtDesc(UUID ownerId);

    Optional<DeviceAgent> findByIdAndOwnerId(UUID id, UUID ownerId);

    Optional<DeviceAgent> findByCredentialHash(String credentialHash);
}
