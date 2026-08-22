package com.nexoia.conversation.chat.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
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

/** A durable, owner-validated Knowledge Vault selection for one conversation. */
@Getter
@Builder
@Entity
@Table(name = "conversation_knowledge_vault")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ConversationKnowledgeVault {

    @Id
    private UUID id;

    @Column(name = "conversation_id", nullable = false)
    private UUID conversationId;

    @Column(name = "vault_id", nullable = false)
    private UUID vaultId;

    @CreationTimestamp
    @Column(name = "selected_at", nullable = false)
    private Instant selectedAt;
}
