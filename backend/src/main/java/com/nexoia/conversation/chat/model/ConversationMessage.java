package com.nexoia.conversation.chat.model;

import com.nexoia.provider.model.ProcessingLocation;
import com.nexoia.provider.model.TokenSource;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.CreationTimestamp;

@Getter
@Builder
@Entity
@Table(name = "conversation_message")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConversationMessage {

    @Id
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "sequence_number", nullable = false)
    private int sequenceNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private ConversationRole role;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 16)
    private MessageStatus status;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String content;

    @Column(name = "provider_configuration_id")
    private UUID providerConfigurationId;

    @Column(length = 160)
    private String model;

    @Column(name = "input_tokens")
    private Integer inputTokens;

    @Column(name = "output_tokens")
    private Integer outputTokens;

    @Enumerated(EnumType.STRING)
    @Column(name = "token_source", length = 16)
    private TokenSource tokenSource;

    @Column(name = "latency_ms")
    private Long latencyMs;

    @Enumerated(EnumType.STRING)
    @Column(name = "processing_location", length = 16)
    private ProcessingLocation processingLocation;

    @Column(name = "failure_code", length = 64)
    private String failureCode;

    @Column(name = "correlation_id")
    private UUID correlationId;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    public void markStreaming() {
        this.status = MessageStatus.STREAMING;
    }

    public void markCancelling() {
        this.status = MessageStatus.CANCELLING;
    }

    public void complete(
            String content,
            Integer inputTokens,
            Integer outputTokens,
            TokenSource tokenSource,
            long latencyMs,
            Instant completedAt) {
        this.status = MessageStatus.COMPLETED;
        this.content = content;
        this.inputTokens = inputTokens;
        this.outputTokens = outputTokens;
        this.tokenSource = tokenSource;
        this.latencyMs = latencyMs;
        this.completedAt = completedAt;
    }

    /**
     * Preserves the text produced before cancellation. The partial answer is kept honestly under the
     * CANCELLED status; it is never promoted to COMPLETED.
     */
    public void cancel(String partialContent, long latencyMs, Instant cancelledAt) {
        this.status = MessageStatus.CANCELLED;
        this.content = partialContent;
        this.latencyMs = latencyMs;
        this.completedAt = cancelledAt;
    }

    public void fail(String failureCode, String partialContent, long latencyMs, Instant failedAt) {
        this.status = MessageStatus.FAILED;
        this.content = partialContent;
        this.failureCode = failureCode;
        this.latencyMs = latencyMs;
        this.completedAt = failedAt;
    }
}
