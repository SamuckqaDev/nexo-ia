package com.nexoia.provider.model;

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
import org.hibernate.annotations.UpdateTimestamp;

@Getter
@Builder
@Entity
@Table(name = "provider_configuration")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProviderConfiguration {
    @Id private UUID id;
    @Column(name = "user_id", nullable = false) private UUID userId;
    @Enumerated(EnumType.STRING) @Column(name = "provider_type", nullable = false, length = 32) private ProviderType providerType;
    @Column(name = "display_name", nullable = false, length = 100) private String displayName;
    @Column(nullable = false, length = 500) private String endpoint;
    @Column(name = "selected_model", length = 160) private String selectedModel;
    @Column(nullable = false) private boolean enabled;
    @Column(name = "last_connected_at") private Instant lastConnectedAt;
    @CreationTimestamp @Column(name = "created_at", nullable = false) private Instant createdAt;
    @UpdateTimestamp @Column(name = "updated_at", nullable = false) private Instant updatedAt;

    public void update(String displayName, String endpoint, String selectedModel, boolean enabled) {
        this.displayName = displayName;
        this.endpoint = endpoint;
        this.selectedModel = selectedModel;
        this.enabled = enabled;
    }

    public void markConnected(Instant connectedAt) { this.lastConnectedAt = connectedAt; }
}
