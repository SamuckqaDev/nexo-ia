package com.nexoia.conversation.chat.service;

import com.nexoia.audit.dto.RecordAuditCommand;
import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditTargetType;
import com.nexoia.audit.service.AuditService;
import com.nexoia.conversation.chat.dto.UpdateConversationKnowledgeRequest;
import com.nexoia.conversation.chat.exception.ConversationNotFoundException;
import com.nexoia.conversation.chat.model.ConversationKnowledgeVault;
import com.nexoia.conversation.chat.repository.ConversationKnowledgeVaultRepository;
import com.nexoia.conversation.chat.repository.ConversationRepository;
import com.nexoia.knowledge.vault.exception.VaultNotFoundException;
import com.nexoia.knowledge.vault.model.KnowledgeVault;
import com.nexoia.knowledge.vault.repository.VaultRepository;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns durable, authorized Knowledge Vault selections for conversations. */
@Service
@RequiredArgsConstructor
public class ConversationKnowledgeService {

    private final ConversationRepository conversations;
    private final ConversationKnowledgeVaultRepository selections;
    private final VaultRepository vaults;
    private final AuditService audit;

    @Transactional
    public List<UUID> replace(
            UUID userId, UUID conversationId, UpdateConversationKnowledgeRequest request) {
        conversations.findOwnedForUpdate(conversationId, userId)
                .orElseThrow(ConversationNotFoundException::new);

        Set<UUID> distinctIds = new LinkedHashSet<>(request.vaultIds());
        List<KnowledgeVault> authorized = distinctIds.isEmpty()
                ? List.of()
                : vaults.findAllByOwnerIdAndArchivedFalseAndIdIn(userId, distinctIds);
        if (authorized.size() != distinctIds.size()) {
            throw new VaultNotFoundException();
        }

        Map<UUID, KnowledgeVault> authorizedById = authorized.stream()
                .collect(Collectors.toMap(KnowledgeVault::getId, vault -> vault));
        selections.deleteAllByConversationId(conversationId);
        selections.flush();
        selections.saveAll(distinctIds.stream()
                .map(authorizedById::get)
                .map(vault -> ConversationKnowledgeVault.builder()
                        .id(UUID.randomUUID())
                        .conversationId(conversationId)
                        .vaultId(vault.getId())
                        .build())
                .toList());

        audit.record(RecordAuditCommand.success(
                AuditAction.CONVERSATION_KNOWLEDGE_SELECTED, userId, null,
                AuditTargetType.CONVERSATION, conversationId));
        return List.copyOf(distinctIds);
    }

    @Transactional(readOnly = true)
    public List<KnowledgeVault> selectedVaults(UUID userId, UUID conversationId) {
        conversations.findByIdAndUserIdAndArchivedFalse(conversationId, userId)
                .orElseThrow(ConversationNotFoundException::new);
        List<UUID> ids = selectionIds(conversationId);
        if (ids.isEmpty()) {
            return List.of();
        }

        Map<UUID, KnowledgeVault> authorizedById = vaults
                .findAllByOwnerIdAndArchivedFalseAndIdIn(userId, ids).stream()
                .collect(Collectors.toMap(KnowledgeVault::getId, vault -> vault));
        return ids.stream().map(authorizedById::get).filter(Objects::nonNull).toList();
    }

    @Transactional(readOnly = true)
    public Map<UUID, List<UUID>> selectionIdsByConversation(List<UUID> conversationIds) {
        if (conversationIds.isEmpty()) {
            return Map.of();
        }
        return selections.findAllByConversationIdIn(conversationIds).stream()
                .collect(Collectors.groupingBy(
                        ConversationKnowledgeVault::getConversationId,
                        Collectors.mapping(ConversationKnowledgeVault::getVaultId, Collectors.toList())));
    }

    @Transactional(readOnly = true)
    public List<UUID> selectionIds(UUID conversationId) {
        return selections.findAllByConversationIdOrderBySelectedAtAsc(conversationId).stream()
                .map(ConversationKnowledgeVault::getVaultId)
                .toList();
    }
}
