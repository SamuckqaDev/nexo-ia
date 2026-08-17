package com.nexoia.auth.credential.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Entity
@Table(name = "password_credential")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
public class PasswordCredential {

    @Id
    @Column(name = "user_id")
    private UUID userId;

    @Column(name = "password_hash", nullable = false, length = 255)
    private String passwordHash;

    @Column(name = "changed_at", nullable = false)
    private Instant changedAt;

    public PasswordCredential(UUID userId, String passwordHash, Instant changedAt) {
        this.userId = userId;
        this.passwordHash = passwordHash;
        this.changedAt = changedAt;
    }

    public void changePassword(String passwordHash, Instant changedAt) {
        this.passwordHash = passwordHash;
        this.changedAt = changedAt;
    }
}
