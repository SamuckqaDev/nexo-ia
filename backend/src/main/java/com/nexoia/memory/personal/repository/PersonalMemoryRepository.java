package com.nexoia.memory.personal.repository;

import com.nexoia.memory.personal.model.PersonalMemory;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PersonalMemoryRepository extends JpaRepository<PersonalMemory, UUID> {

    List<PersonalMemory> findAllByUserIdOrderByUpdatedAtDesc(UUID userId);

    Optional<PersonalMemory> findFirstByUserIdAndContentIgnoreCase(UUID userId, String content);

    Optional<PersonalMemory> findByIdAndUserId(UUID id, UUID userId);

    long countByUserId(UUID userId);
}
