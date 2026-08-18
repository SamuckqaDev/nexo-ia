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
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Builder
@Entity
@Table(name = "conversation")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class Conversation {

    @Id
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(nullable = false, length = 160)
    private String title;

    @Column(name = "provider_configuration_id")
    private UUID providerConfigurationId;

    @Column(name = "selected_model", length = 160)
    private String selectedModel;

    @Column(nullable = false)
    private boolean archived;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void selectModel(UUID providerConfigurationId, String selectedModel) {
        this.providerConfigurationId = providerConfigurationId;
        this.selectedModel = selectedModel;
    }

    public void rename(String title) {
        this.title = title;
    }

    public void archive() {
        this.archived = true;
    }

    public boolean hasSelectedModel() {
        return providerConfigurationId != null && selectedModel != null && !selectedModel.isBlank();
    }
}
