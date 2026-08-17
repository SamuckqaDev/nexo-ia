package com.nexoia.auth.loginattempt.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.Instant;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@Builder
@Entity
@Table(name = "login_attempt")
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@AllArgsConstructor(access = AccessLevel.PRIVATE)
public class LoginAttempt {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "identifier_hash", nullable = false, length = 64)
    private String identifierHash;
    @Column(name = "ip_address", nullable = false, length = 45)
    private String ipAddress;
    @Column(nullable = false)
    private boolean successful;
    @Column(name = "occurred_at", nullable = false)
    private Instant occurredAt;
}
