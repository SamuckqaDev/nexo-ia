package com.nexoia.conversation.chat.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.audit.service.AuditService;
import com.nexoia.conversation.chat.dto.UpdateConversationKnowledgeRequest;
import com.nexoia.conversation.chat.model.Conversation;
import com.nexoia.conversation.chat.model.ConversationKnowledgeVault;
import com.nexoia.conversation.chat.repository.ConversationKnowledgeVaultRepository;
import com.nexoia.conversation.chat.repository.ConversationRepository;
import com.nexoia.knowledge.vault.exception.VaultNotFoundException;
import com.nexoia.knowledge.vault.model.KnowledgeVault;
import com.nexoia.knowledge.vault.model.VaultScope;
import com.nexoia.knowledge.vault.repository.VaultRepository;
import com.nexoia.team.service.TeamMembershipService;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ConversationKnowledgeServiceTest {

    @Mock private ConversationRepository conversations;
    @Mock private ConversationKnowledgeVaultRepository selections;
    @Mock private VaultRepository vaults;
    @Mock private TeamMembershipService teamMembershipService;
    @Mock private AuditService audit;

    private ConversationKnowledgeService service;
    private final UUID userId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ConversationKnowledgeService(
                conversations, selections, vaults, teamMembershipService, audit);
    }

    @Test
    void atomicallyReplacesOnlyVaultsOwnedByTheConversationOwner() {
        KnowledgeVault first = vault("First");
        KnowledgeVault second = vault("Second");
        when(conversations.findOwnedForUpdate(conversationId, userId))
                .thenReturn(Optional.of(conversation()));
        when(teamMembershipService.accessibleOwnerIds(userId)).thenReturn(List.of(userId));
        when(vaults.findAllByOwnerIdInAndArchivedFalseAndIdIn(any(), any(Iterable.class)))
                .thenReturn(List.of(first, second));

        List<UUID> result = service.replace(
                userId,
                conversationId,
                new UpdateConversationKnowledgeRequest(List.of(first.getId(), second.getId())));

        assertThat(result).containsExactly(first.getId(), second.getId());
        verify(selections).deleteAllByConversationId(conversationId);
        ArgumentCaptor<List<ConversationKnowledgeVault>> saved = ArgumentCaptor.forClass(List.class);
        verify(selections).saveAll(saved.capture());
        assertThat(saved.getValue()).extracting(ConversationKnowledgeVault::getVaultId)
                .containsExactly(first.getId(), second.getId());
    }

    @Test
    void rejectsTheWholeSelectionWhenAnyVaultIsForeignOrArchived() {
        UUID owned = UUID.randomUUID();
        UUID foreign = UUID.randomUUID();
        when(conversations.findOwnedForUpdate(conversationId, userId))
                .thenReturn(Optional.of(conversation()));
        when(teamMembershipService.accessibleOwnerIds(userId)).thenReturn(List.of(userId));
        when(vaults.findAllByOwnerIdInAndArchivedFalseAndIdIn(any(), any(Iterable.class)))
                .thenReturn(List.of(KnowledgeVault.builder()
                        .id(owned)
                        .ownerId(userId)
                        .name("Owned")
                        .scope(VaultScope.PERSONAL)
                        .build()));

        assertThatThrownBy(() -> service.replace(
                userId,
                conversationId,
                new UpdateConversationKnowledgeRequest(List.of(owned, foreign))))
                .isInstanceOf(VaultNotFoundException.class);

        verify(selections, never()).deleteAllByConversationId(any());
        verify(selections, never()).saveAll(any());
    }

    private Conversation conversation() {
        return Conversation.builder()
                .id(conversationId)
                .userId(userId)
                .title("Chat")
                .build();
    }

    private KnowledgeVault vault(String name) {
        return KnowledgeVault.builder()
                .id(UUID.randomUUID())
                .ownerId(userId)
                .name(name)
                .scope(VaultScope.PERSONAL)
                .build();
    }
}
