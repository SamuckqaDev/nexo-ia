package com.nexoia.usage.repository;

import com.nexoia.conversation.chat.model.ConversationMessage;
import com.nexoia.usage.dto.UsageLocationBreakdown;
import com.nexoia.usage.dto.UsageModelBreakdown;
import com.nexoia.usage.dto.UsageTotals;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * Reads usage straight from the recorded assistant messages.
 *
 * <p>There is no separate usage_record table yet: every counter release 0.1 reports already lives on
 * the message that produced it, and duplicating it would create a second source of truth to keep
 * consistent. A dedicated record becomes justified when work that is not a chat message — an agent
 * run or an image job — also needs attribution.
 *
 * <p>Ownership is enforced inside each query through the conversation subselect, never by filtering
 * results afterwards.
 *
 * <p>Every aggregate is wrapped in {@code coalesce}: SQL {@code sum} over zero rows returns null,
 * and the response record uses primitives, so an empty reporting window would otherwise fail.
 */
public interface UsageRepository extends JpaRepository<ConversationMessage, UUID> {

    @Query("""
            SELECT new com.nexoia.usage.dto.UsageTotals(
                count(m),
                coalesce(sum(CASE WHEN m.status = com.nexoia.conversation.chat.model.MessageStatus.COMPLETED THEN 1L ELSE 0L END), 0L),
                coalesce(sum(CASE WHEN m.status = com.nexoia.conversation.chat.model.MessageStatus.CANCELLED THEN 1L ELSE 0L END), 0L),
                coalesce(sum(CASE WHEN m.status = com.nexoia.conversation.chat.model.MessageStatus.FAILED THEN 1L ELSE 0L END), 0L),
                coalesce(sum(m.inputTokens), 0L),
                coalesce(sum(m.outputTokens), 0L),
                coalesce(sum(m.inputTokens), 0L) + coalesce(sum(m.outputTokens), 0L),
                avg(m.latencyMs),
                coalesce(sum(CASE WHEN m.tokenSource = com.nexoia.provider.model.TokenSource.ESTIMATE THEN 1L ELSE 0L END), 0L))
            FROM ConversationMessage m
            WHERE m.role = com.nexoia.conversation.chat.model.ConversationRole.ASSISTANT
              AND m.createdAt >= :from
              AND m.conversationId IN (SELECT c.id FROM Conversation c WHERE c.userId = :userId)
            """)
    UsageTotals totals(@Param("userId") UUID userId, @Param("from") Instant from);

    @Query("""
            SELECT new com.nexoia.usage.dto.UsageModelBreakdown(
                m.model,
                count(m),
                coalesce(sum(m.inputTokens), 0L),
                coalesce(sum(m.outputTokens), 0L),
                avg(m.latencyMs))
            FROM ConversationMessage m
            WHERE m.role = com.nexoia.conversation.chat.model.ConversationRole.ASSISTANT
              AND m.model IS NOT NULL
              AND m.createdAt >= :from
              AND m.conversationId IN (SELECT c.id FROM Conversation c WHERE c.userId = :userId)
            GROUP BY m.model
            ORDER BY count(m) DESC
            """)
    List<UsageModelBreakdown> byModel(@Param("userId") UUID userId, @Param("from") Instant from);

    @Query("""
            SELECT new com.nexoia.usage.dto.UsageLocationBreakdown(
                m.processingLocation,
                count(m),
                coalesce(sum(m.inputTokens), 0L) + coalesce(sum(m.outputTokens), 0L))
            FROM ConversationMessage m
            WHERE m.role = com.nexoia.conversation.chat.model.ConversationRole.ASSISTANT
              AND m.processingLocation IS NOT NULL
              AND m.createdAt >= :from
              AND m.conversationId IN (SELECT c.id FROM Conversation c WHERE c.userId = :userId)
            GROUP BY m.processingLocation
            ORDER BY count(m) DESC
            """)
    List<UsageLocationBreakdown> byProcessingLocation(
            @Param("userId") UUID userId, @Param("from") Instant from);

    @Query("""
            SELECT m.createdAt, m.inputTokens, m.outputTokens
            FROM ConversationMessage m
            WHERE m.role = com.nexoia.conversation.chat.model.ConversationRole.ASSISTANT
              AND m.createdAt >= :from
              AND m.conversationId IN (SELECT c.id FROM Conversation c WHERE c.userId = :userId)
            ORDER BY m.createdAt ASC
            """)
    List<Object[]> dailyRows(@Param("userId") UUID userId, @Param("from") Instant from);
}
