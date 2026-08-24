package com.nexoia.memory.personal.tool;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.audit.service.AuditService;
import com.nexoia.memory.personal.dto.PersonalMemoryResponse;
import com.nexoia.memory.personal.service.PersonalMemoryService;
import com.nexoia.provider.dto.MemoryToolScope;
import com.nexoia.provider.dto.ToolExecutionObserver;
import com.nexoia.provider.dto.ToolExecutionStatus;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RememberToolFactoryTest {

    @Test
    void storesMemoryOnlyUnderTheServerCreatedOwnerScope() {
        PersonalMemoryService memories = mock(PersonalMemoryService.class);
        MemoryToolScope scope = new MemoryToolScope(
                UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID());
        UUID memoryId = UUID.randomUUID();
        when(memories.remember(scope.userId(), "The user prefers concise answers.",
                scope.conversationId(), scope.assistantMessageId()))
                .thenReturn(new PersonalMemoryResponse(
                        memoryId, "The user prefers concise answers.", scope.conversationId(), null, null));
        RememberToolFactory factory = new RememberToolFactory(
                memories, mock(AuditService.class),
                Clock.fixed(Instant.parse("2026-08-24T12:00:00Z"), ZoneOffset.UTC));
        RememberToolSession session = factory.open(scope, ToolExecutionObserver.NOOP, () -> false);

        String result = session.callback().call("""
                {"content":"The user prefers concise answers."}
                """);

        assertThat(result).contains("COMPLETED").contains(memoryId.toString());
        assertThat(session.evidence()).singleElement()
                .satisfies(evidence -> assertThat(evidence.status()).isEqualTo(ToolExecutionStatus.COMPLETED));
        assertThat(session.callback().getToolDefinition().inputSchema())
                .doesNotContain("userId", "conversationId", "assistantMessageId");
        verify(memories).remember(scope.userId(), "The user prefers concise answers.",
                scope.conversationId(), scope.assistantMessageId());
    }

    @Test
    void deniesBlankMemoryWithoutWriting() {
        PersonalMemoryService memories = mock(PersonalMemoryService.class);
        RememberToolFactory factory = new RememberToolFactory(
                memories, mock(AuditService.class), Clock.systemUTC());
        RememberToolSession session = factory.open(
                new MemoryToolScope(UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID(), UUID.randomUUID()),
                ToolExecutionObserver.NOOP, () -> false);

        String result = session.callback().call("{\"content\":\"   \"}");

        assertThat(result).contains("DENIED");
    }
}
