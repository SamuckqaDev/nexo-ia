package com.nexoia.audit.service;

import com.nexoia.audit.dto.AuditEventResponse;
import com.nexoia.audit.dto.RecordAuditCommand;
import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditEvent;
import com.nexoia.audit.repository.AuditEventRepository;
import java.time.Clock;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Limit;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Records and reads the unified audit trail.
 *
 * <p>Recording runs in the caller's transaction: an action that rolls back must not leave a
 * "succeeded" audit behind, and a committed action is audited atomically with it. The trail never
 * stores secrets or message content — callers pass a short, safe {@code detail} only.
 */
@Service
@RequiredArgsConstructor
public class AuditService {

    private static final int MAX_DETAIL_LENGTH = 256;
    private static final int DEFAULT_LIMIT = 100;
    private static final int MAX_LIMIT = 500;

    private final AuditEventRepository repository;
    private final Clock clock;

    @Transactional
    public void record(RecordAuditCommand command) {
        repository.save(AuditEvent.builder()
                .id(UUID.randomUUID())
                .action(command.action())
                .outcome(command.outcome())
                .actorUserId(command.actorUserId())
                .actorRole(command.actorRole())
                .targetType(command.targetType())
                .targetId(command.targetId())
                .correlationId(command.correlationId())
                .detail(truncate(command.detail()))
                .occurredAt(clock.instant())
                .build());
    }

    @Transactional(readOnly = true)
    public List<AuditEventResponse> query(Optional<AuditAction> action, Optional<UUID> actorUserId, int limit) {
        Limit bounded = Limit.of(Math.clamp(limit, 1, MAX_LIMIT));

        List<AuditEvent> events;
        if (action.isPresent() && actorUserId.isPresent()) {
            events = repository.findAllByActionAndActor(action.get(), actorUserId.get(), bounded);
        } else if (action.isPresent()) {
            events = repository.findAllByActionOrderByOccurredAtDesc(action.get(), bounded);
        } else if (actorUserId.isPresent()) {
            events = repository.findAllByActorUserIdOrderByOccurredAtDesc(actorUserId.get(), bounded);
        } else {
            events = repository.findAllByOrderByOccurredAtDesc(bounded);
        }

        return events.stream().map(this::response).toList();
    }

    public int defaultLimit() {
        return DEFAULT_LIMIT;
    }

    private String truncate(String detail) {
        if (detail == null) {
            return null;
        }
        String trimmed = detail.strip();
        return trimmed.length() <= MAX_DETAIL_LENGTH ? trimmed : trimmed.substring(0, MAX_DETAIL_LENGTH);
    }

    private AuditEventResponse response(AuditEvent event) {
        return new AuditEventResponse(
                event.getId(),
                event.getAction(),
                event.getOutcome(),
                event.getActorUserId(),
                event.getActorRole(),
                event.getTargetType(),
                event.getTargetId(),
                event.getCorrelationId(),
                event.getDetail(),
                event.getOccurredAt());
    }
}
