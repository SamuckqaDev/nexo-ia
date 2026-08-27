package com.nexoia.device.repository;

import com.nexoia.device.model.DevicePairing;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

public interface DevicePairingRepository extends JpaRepository<DevicePairing, UUID> {

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<DevicePairing> findByCodeHash(String codeHash);
}
