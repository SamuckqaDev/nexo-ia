package com.nexoia.usage.repository;

import static org.assertj.core.api.Assertions.assertThat;

import com.nexoia.conversation.chat.model.Conversation;
import com.nexoia.conversation.chat.model.ConversationMessage;
import com.nexoia.conversation.chat.model.ConversationRole;
import com.nexoia.conversation.chat.model.MessageStatus;
import com.nexoia.provider.model.ProcessingLocation;
import com.nexoia.provider.model.TokenSource;
import com.nexoia.usage.dto.UsageModelBreakdown;
import com.nexoia.NexoApplication;
import com.nexoia.usage.dto.UsageTotals;
import java.time.Instant;
import java.util.List;
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
import org.testcontainers.utility.DockerImageName;

/**
 * Usage aggregation runs entirely in the database, so it is verified against a real PostgreSQL
 * instance. The isolation assertions matter most: a reporting query that forgets its ownership
 * filter leaks another member's private activity without any visible error.
 */
@Tag("docker")
@Testcontainers
@SpringBootTest(classes = NexoApplication.class, properties = {
        "nexo.security.token.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA="
})
class UsageRepositoryTest {

    @Container
    @ServiceConnection
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>(
            DockerImageName.parse("pgvector/pgvector:0.8.6-pg18-bookworm")
                    .asCompatibleSubstituteFor("postgres"));

    @Autowired
    private UsageRepository repository;
    @Autowired
    private JdbcTemplate jdbc;

    private final UUID owner = UUID.randomUUID();
    private final UUID otherMember = UUID.randomUUID();
    private UUID ownerConversation;
    private UUID otherConversation;

    @BeforeEach
    void setUp() {
        jdbc.update("DELETE FROM conversation_message");
        jdbc.update("DELETE FROM conversation");
        jdbc.update("DELETE FROM user_account");

        insertUser(owner, "owner", "owner@nexo.local", "OWNER");
        insertUser(otherMember, "member", "member@nexo.local", "MEMBER");
        ownerConversation = insertConversation(owner, "Owner planning");
        otherConversation = insertConversation(otherMember, "Member planning");
    }

    @Test
    void aggregatesOnlyTheRequestingMembersOwnRequests() {
        insertAssistantMessage(ownerConversation, 1, MessageStatus.COMPLETED, "qwen3:8b", 20, 3, 1500);
        insertAssistantMessage(ownerConversation, 2, MessageStatus.COMPLETED, "qwen3:8b", 30, 7, 2500);
        insertAssistantMessage(otherConversation, 1, MessageStatus.COMPLETED, "gemma4:12b", 999, 999, 9999);

        UsageTotals totals = repository.totals(owner, Instant.EPOCH);

        assertThat(totals.requests()).isEqualTo(2);
        assertThat(totals.inputTokens()).isEqualTo(50);
        assertThat(totals.outputTokens()).isEqualTo(10);
        assertThat(totals.totalTokens()).isEqualTo(60);
        assertThat(totals.averageLatencyMs()).isEqualTo(2000.0);
    }

    @Test
    void neverReportsAnotherMembersModelsInTheBreakdown() {
        insertAssistantMessage(ownerConversation, 1, MessageStatus.COMPLETED, "qwen3:8b", 20, 3, 1000);
        insertAssistantMessage(otherConversation, 1, MessageStatus.COMPLETED, "gemma4:12b", 40, 6, 1000);

        List<UsageModelBreakdown> breakdown = repository.byModel(owner, Instant.EPOCH);

        assertThat(breakdown).hasSize(1);
        assertThat(breakdown.getFirst().model()).isEqualTo("qwen3:8b");
        assertThat(breakdown.getFirst().requests()).isEqualTo(1);
    }

    @Test
    void countsTerminalStatesSeparatelySoFailuresAreNotHiddenInTheTotal() {
        insertAssistantMessage(ownerConversation, 1, MessageStatus.COMPLETED, "qwen3:8b", 10, 2, 900);
        insertAssistantMessage(ownerConversation, 2, MessageStatus.CANCELLED, "qwen3:8b", null, null, 400);
        insertAssistantMessage(ownerConversation, 3, MessageStatus.FAILED, "qwen3:8b", null, null, 100);

        UsageTotals totals = repository.totals(owner, Instant.EPOCH);

        assertThat(totals.requests()).isEqualTo(3);
        assertThat(totals.completed()).isEqualTo(1);
        assertThat(totals.cancelled()).isEqualTo(1);
        assertThat(totals.failed()).isEqualTo(1);
        assertThat(totals.totalTokens()).isEqualTo(12);
    }

    @Test
    void reportsAnEmptyWindowAsZeroInsteadOfNull() {
        insertAssistantMessage(ownerConversation, 1, MessageStatus.COMPLETED, "qwen3:8b", 10, 2, 900);

        UsageTotals totals = repository.totals(owner, Instant.now().plusSeconds(60));

        assertThat(totals.requests()).isZero();
        assertThat(totals.totalTokens()).isZero();
        assertThat(totals.averageLatencyMs()).isNull();
    }

    @Test
    void separatesLocalFromRemoteProcessing() {
        insertAssistantMessage(ownerConversation, 1, MessageStatus.COMPLETED, "qwen3:8b", 10, 2, 100);
        insertAssistantMessage(ownerConversation, 2, MessageStatus.COMPLETED, "qwen3:8b", 10, 2, 100);

        assertThat(repository.byProcessingLocation(owner, Instant.EPOCH))
                .singleElement()
                .satisfies(entry -> {
                    assertThat(entry.processingLocation()).isEqualTo(ProcessingLocation.LOCAL);
                    assertThat(entry.requests()).isEqualTo(2);
                    assertThat(entry.totalTokens()).isEqualTo(24);
                });
    }

    @Test
    void countsEstimatedTokenRequestsSeparatelyFromProviderReportedOnes() {
        insertAssistantMessage(ownerConversation, 1, MessageStatus.COMPLETED, "qwen3:8b", 10, 2, 100);
        jdbc.update("""
                INSERT INTO conversation_message
                    (id, conversation_id, sequence_number, role, status, content, model,
                     input_tokens, output_tokens, token_source, latency_ms, processing_location, created_at)
                VALUES (?, ?, 2, 'ASSISTANT', 'COMPLETED', 'estimated', 'qwen3:8b',
                        5, 1, ?, 200, 'LOCAL', now())
                """, UUID.randomUUID(), ownerConversation, TokenSource.ESTIMATE.name());

        assertThat(repository.totals(owner, Instant.EPOCH).estimatedTokenRequests()).isEqualTo(1);
    }

    private void insertUser(UUID id, String username, String email, String role) {
        jdbc.update("""
                INSERT INTO user_account (id, username, email, display_name, role, status, created_at, updated_at)
                VALUES (?, ?, ?, ?, ?, 'ACTIVE', now(), now())
                """, id, username, email, username, role);
    }

    private UUID insertConversation(UUID userId, String title) {
        UUID id = UUID.randomUUID();
        jdbc.update("""
                INSERT INTO conversation (id, user_id, title, archived, created_at, updated_at)
                VALUES (?, ?, ?, false, now(), now())
                """, id, userId, title);

        return id;
    }

    private void insertAssistantMessage(
            UUID conversationId, int sequence, MessageStatus status, String model,
            Integer inputTokens, Integer outputTokens, long latencyMs) {
        jdbc.update("""
                INSERT INTO conversation_message
                    (id, conversation_id, sequence_number, role, status, content, model,
                     input_tokens, output_tokens, token_source, latency_ms, processing_location, created_at)
                VALUES (?, ?, ?, ?, ?, 'answer', ?, ?, ?, ?, ?, 'LOCAL', now())
                """,
                UUID.randomUUID(), conversationId, sequence, ConversationRole.ASSISTANT.name(),
                status.name(), model, inputTokens, outputTokens,
                inputTokens == null ? null : TokenSource.PROVIDER.name(), latencyMs);
    }
}
