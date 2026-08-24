package com.nexoia.memory.personal.service;

import com.nexoia.audit.dto.RecordAuditCommand;
import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditTargetType;
import com.nexoia.audit.service.AuditService;
import com.nexoia.memory.personal.dto.PersonalMemoryResponse;
import com.nexoia.memory.personal.exception.PersonalMemoryLimitException;
import com.nexoia.memory.personal.exception.PersonalMemoryNotFoundException;
import com.nexoia.memory.personal.model.PersonalMemory;
import com.nexoia.memory.personal.repository.PersonalMemoryRepository;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PersonalMemoryService {

    public static final int MAX_MEMORIES = 50;
    public static final int CONTEXT_LIMIT = 20;

    private final PersonalMemoryRepository memories;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public List<PersonalMemoryResponse> list(UUID userId) {
        return memories.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(this::response)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<String> context(UUID userId) {
        return memories.findAllByUserIdOrderByUpdatedAtDesc(userId).stream()
                .limit(CONTEXT_LIMIT)
                .map(PersonalMemory::getContent)
                .toList();
    }

    @Transactional
    public PersonalMemoryResponse remember(
            UUID userId, String content, UUID sourceConversationId, UUID sourceMessageId) {
        String normalized = normalize(content);
        return memories.findFirstByUserIdAndContentIgnoreCase(userId, normalized)
                .map(this::response)
                .orElseGet(() -> create(userId, normalized, sourceConversationId, sourceMessageId));
    }

    @Transactional
    public void remove(UUID userId, UUID memoryId) {
        PersonalMemory memory = memories.findByIdAndUserId(memoryId, userId)
                .orElseThrow(PersonalMemoryNotFoundException::new);
        memories.delete(memory);
        audit.record(RecordAuditCommand.success(
                AuditAction.MEMORY_REMOVED, userId, null, AuditTargetType.MEMORY, memoryId));
    }

    private PersonalMemoryResponse create(
            UUID userId, String content, UUID sourceConversationId, UUID sourceMessageId) {
        if (memories.countByUserId(userId) >= MAX_MEMORIES) {
            throw new PersonalMemoryLimitException();
        }
        PersonalMemory memory = memories.saveAndFlush(PersonalMemory.builder()
                .id(UUID.randomUUID())
                .userId(userId)
                .content(content)
                .sourceConversationId(sourceConversationId)
                .sourceMessageId(sourceMessageId)
                .build());
        audit.record(RecordAuditCommand.success(
                AuditAction.MEMORY_CREATED, userId, null, AuditTargetType.MEMORY, memory.getId()));
        return response(memory);
    }

    private String normalize(String content) {
        if (content == null) {
            return "";
        }
        return content.trim().replaceAll("\\s+", " ");
    }

    private PersonalMemoryResponse response(PersonalMemory memory) {
        return new PersonalMemoryResponse(
                memory.getId(), memory.getContent(), memory.getSourceConversationId(),
                memory.getCreatedAt(), memory.getUpdatedAt());
    }
}
