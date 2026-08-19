package com.nexoia.audit.repository;

import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditEvent;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Limit;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AuditEventRepository extends JpaRepository<AuditEvent, UUID> {

    List<AuditEvent> findAllByOrderByOccurredAtDesc(Limit limit);

    List<AuditEvent> findAllByActionOrderByOccurredAtDesc(AuditAction action, Limit limit);

    List<AuditEvent> findAllByActorUserIdOrderByOccurredAtDesc(UUID actorUserId, Limit limit);

    @Query("""
            SELECT e FROM AuditEvent e
            WHERE e.action = :action AND e.actorUserId = :actorUserId
            ORDER BY e.occurredAt DESC
            """)
    List<AuditEvent> findAllByActionAndActor(
            @Param("action") AuditAction action,
            @Param("actorUserId") UUID actorUserId,
            Limit limit);
}
