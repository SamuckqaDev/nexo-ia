package com.nexoia.audit.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexoia.NexoApplication;
import com.nexoia.audit.dto.RecordAuditCommand;
import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditEvent;
import com.nexoia.audit.model.AuditOutcome;
import com.nexoia.audit.model.AuditTargetType;
import com.nexoia.audit.service.AuditService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.jdbc.core.JdbcTemplate;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * The audit query runs in the database, so it is verified against a real PostgreSQL instance,
 * including the ordering and the action and actor filters the admin trail relies on.
 */
@Tag("docker")
@Testcontainers
@SpringBootTest(classes = NexoApplication.class, properties = {
        "nexo.security.token.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
class AuditEventRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18.4");

    @Autowired
    private AuditService service;
    @Autowired
    private AuditEventRepository repository;
    @Autowired
    private JdbcTemplate jdbc;

    private final UUID owner = UUID.randomUUID();
    private final UUID member = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM audit_event");
        jdbc.update("DELETE FROM user_account");
        insertUser(owner, "owner", "owner@nexo.local", "OWNER");
        insertUser(member, "member", "member@nexo.local", "MEMBER");
    }

    @Test
    void returnsTheMostRecentEventsFirst() {
        service.record(RecordAuditCommand.success(
                AuditAction.PROVIDER_CREATED, member, null, AuditTargetType.PROVIDER, UUID.randomUUID()));
        service.record(RecordAuditCommand.success(
                AuditAction.CONVERSATION_CREATED, member, null, AuditTargetType.CONVERSATION, UUID.randomUUID()));

        List<AuditEvent> events = repository.findAllByOrderByOccurredAtDesc(
                org.springframework.data.domain.Limit.of(10));

        assertThat(events).hasSize(2);
        assertThat(events.getFirst().getAction()).isEqualTo(AuditAction.CONVERSATION_CREATED);
    }

    @Test
    void filtersByActionAndByActor() {
        service.record(RecordAuditCommand.success(
                AuditAction.PROVIDER_CREATED, owner, null, AuditTargetType.PROVIDER, UUID.randomUUID()));
        service.record(RecordAuditCommand.success(
                AuditAction.PROVIDER_CREATED, member, null, AuditTargetType.PROVIDER, UUID.randomUUID()));
        service.record(RecordAuditCommand.success(
                AuditAction.CONVERSATION_CREATED, member, null, AuditTargetType.CONVERSATION, UUID.randomUUID()));

        assertThat(service.query(Optional.of(AuditAction.PROVIDER_CREATED), Optional.empty(), 50)).hasSize(2);
        assertThat(service.query(Optional.empty(), Optional.of(member), 50)).hasSize(2);
        assertThat(service.query(
                Optional.of(AuditAction.PROVIDER_CREATED), Optional.of(member), 50)).hasSize(1);
    }

    @Test
    void persistsTheCorrelationIdThatTiesAModelRequestToItsMessage() {
        UUID correlationId = UUID.randomUUID();
        UUID messageId = UUID.randomUUID();
        service.record(new RecordAuditCommand(
                AuditAction.MODEL_REQUEST_COMPLETED, AuditOutcome.SUCCESS, member, null,
                AuditTargetType.MESSAGE, messageId, correlationId, null));

        AuditEvent event = repository.findAllByOrderByOccurredAtDesc(
                org.springframework.data.domain.Limit.of(1)).getFirst();

        assertThat(event.getCorrelationId()).isEqualTo(correlationId);
        assertThat(event.getTargetId()).isEqualTo(messageId);
    }

    private void insertUser(UUID id, String username, String email, String role) {
        jdbc.update("""
                INSERT INTO user_account (id, username, email, display_name, role, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE', now(), now())
                """, id, username, email, username, role);
    }
}
