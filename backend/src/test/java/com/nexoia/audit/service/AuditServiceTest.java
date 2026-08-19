package com.nexoia.audit.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.audit.dto.RecordAuditCommand;
import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditEvent;
import com.nexoia.audit.model.AuditOutcome;
import com.nexoia.audit.model.AuditTargetType;
import com.nexoia.audit.repository.AuditEventRepository;
import com.nexoia.auth.user.model.UserRole;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

    @Mock
    private AuditEventRepository repository;
    private final Clock clock = Clock.fixed(Instant.parse("2026-08-19T10:00:00Z"), ZoneOffset.UTC);

    @Test
    void stampsTheOccurrenceTimeAndPersistsTheCommand() {
        AuditService service = new AuditService(repository, clock);
        UUID actor = UUID.randomUUID();
        UUID target = UUID.randomUUID();

        service.record(RecordAuditCommand.success(
                AuditAction.PROVIDER_CREATED, actor, UserRole.OWNER, AuditTargetType.PROVIDER, target));

        ArgumentCaptor<AuditEvent> saved = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getAction()).isEqualTo(AuditAction.PROVIDER_CREATED);
        assertThat(saved.getValue().getOutcome()).isEqualTo(AuditOutcome.SUCCESS);
        assertThat(saved.getValue().getActorUserId()).isEqualTo(actor);
        assertThat(saved.getValue().getTargetId()).isEqualTo(target);
        assertThat(saved.getValue().getOccurredAt()).isEqualTo(Instant.parse("2026-08-19T10:00:00Z"));
    }

    @Test
    void truncatesAnOverlongDetailSoTheColumnBoundIsNeverExceeded() {
        AuditService service = new AuditService(repository, clock);

        service.record(new RecordAuditCommand(
                AuditAction.CONVERSATION_MODEL_SELECTED, AuditOutcome.SUCCESS, UUID.randomUUID(), null,
                AuditTargetType.CONVERSATION, UUID.randomUUID(), null, "x".repeat(400)));

        ArgumentCaptor<AuditEvent> saved = ArgumentCaptor.forClass(AuditEvent.class);
        verify(repository).save(saved.capture());
        assertThat(saved.getValue().getDetail()).hasSize(256);
    }

    @Test
    void clampsTheRequestedLimitToTheAllowedRange() {
        AuditService service = new AuditService(repository, clock);
        when(repository.findAllByOrderByOccurredAtDesc(any())).thenReturn(java.util.List.of());

        service.query(java.util.Optional.empty(), java.util.Optional.empty(), 100000);

        verify(repository).findAllByOrderByOccurredAtDesc(org.springframework.data.domain.Limit.of(500));
    }
}
