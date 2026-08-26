package com.nexoia.conversation.inference.model;

import com.nexoia.provider.dto.AgentPlanUpdate;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** Latest durable revision of the implementation plan for one Agent response. */
@Getter
@Builder
@Entity
@Table(name = "agent_plan")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class AgentPlanRecord {

    @Id
    private UUID id;

    @Column(name = "assistant_message_id", nullable = false, unique = true)
    private UUID assistantMessageId;

    @Column(nullable = false)
    private int revision;

    @Column(length = 500)
    private String explanation;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(nullable = false, columnDefinition = "jsonb")
    private List<AgentPlanStep> steps;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void update(AgentPlanUpdate update) {
        revision = update.revision();
        explanation = update.explanation();
        steps = update.steps().stream()
                .map(step -> new AgentPlanStep(step.step(), step.description(), step.status()))
                .toList();
        updatedAt = update.updatedAt();
    }
}
