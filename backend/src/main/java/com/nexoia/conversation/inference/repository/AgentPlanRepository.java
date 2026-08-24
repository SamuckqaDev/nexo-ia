package com.nexoia.conversation.inference.repository;

import com.nexoia.conversation.inference.model.AgentPlanRecord;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AgentPlanRepository extends JpaRepository<AgentPlanRecord, UUID> {

    Optional<AgentPlanRecord> findByAssistantMessageId(UUID assistantMessageId);

    List<AgentPlanRecord> findAllByAssistantMessageIdIn(Collection<UUID> assistantMessageIds);
}
