package com.nexoia.conversation.inference.model;

import com.nexoia.knowledge.retrieval.dto.CitationResponse;
import com.nexoia.provider.dto.ToolExecutionEvidence;
import com.nexoia.provider.dto.ToolExecutionStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Durable, sanitized evidence for one governed tool execution. */
@Getter
@Builder
@Entity
@Table(name = "tool_execution")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ToolExecutionRecord {

    @Id
    private UUID id;

    @Column(name = "assistant_message_id", nullable = false)
    private UUID assistantMessageId;

    @Column(name = "correlation_id", nullable = false)
    private UUID correlationId;

    @Column(name = "tool_name", nullable = false, length = 80)
    private String toolName;

    @Column(name = "arguments_digest", nullable = false, length = 64)
    private String argumentsDigest;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 24)
    private ToolExecutionStatus status;

    @Column(name = "duration_ms")
    private Long durationMs;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(columnDefinition = "jsonb")
    private List<CitationResponse> citations;

    @Column(name = "started_at", nullable = false)
    private Instant startedAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public void complete(ToolExecutionEvidence evidence) {
        status = evidence.status();
        durationMs = evidence.durationMs();
        citations = evidence.citations().isEmpty() ? null : evidence.citations();
        completedAt = evidence.completedAt();
    }
}
