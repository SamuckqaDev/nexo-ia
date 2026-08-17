package com.nexoia.auth.access.repository;

import com.nexoia.auth.access.model.AccessEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AccessEventRepository extends JpaRepository<AccessEvent, Long> {

    List<AccessEvent> findTop100ByUserIdOrderByOccurredAtDesc(UUID userId);
}
