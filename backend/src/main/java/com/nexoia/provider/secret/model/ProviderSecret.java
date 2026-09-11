package com.nexoia.provider.secret.model;

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
@Table(name = "provider_secret")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class ProviderSecret {

    @Id
    @Column(name = "provider_configuration_id")
    private UUID providerConfigurationId;

    @Column(name = "encrypted_value", nullable = false, columnDefinition = "TEXT")
    private String encryptedValue;

    @Column(name = "initialization_vector", nullable = false, length = 32)
    private String initializationVector;

    @Column(name = "key_version", nullable = false)
    private int keyVersion;

    @CreationTimestamp
    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @UpdateTimestamp
    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public void replace(String encryptedValue, String initializationVector, int keyVersion) {
        this.encryptedValue = encryptedValue;
        this.initializationVector = initializationVector;
        this.keyVersion = keyVersion;
    }
}
