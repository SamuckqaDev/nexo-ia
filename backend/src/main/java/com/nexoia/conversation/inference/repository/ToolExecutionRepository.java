package com.nexoia.conversation.inference.repository;

import com.nexoia.conversation.inference.model.ToolExecutionRecord;
import java.util.Collection;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ToolExecutionRepository extends JpaRepository<ToolExecutionRecord, UUID> {

    List<ToolExecutionRecord> findAllByAssistantMessageIdInOrderByStartedAtAsc(
            Collection<UUID> assistantMessageIds);
}
