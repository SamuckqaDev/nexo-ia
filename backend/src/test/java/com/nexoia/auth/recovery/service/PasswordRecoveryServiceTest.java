package com.nexoia.auth.recovery.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.auth.credential.model.PasswordCredential;
import com.nexoia.auth.credential.repository.PasswordCredentialRepository;
import com.nexoia.auth.recovery.config.PasswordRecoveryProperties;
import com.nexoia.auth.recovery.dto.IssuedPasswordResetToken;
import com.nexoia.auth.recovery.dto.PasswordResetConfirmation;
import com.nexoia.auth.recovery.model.PasswordResetToken;
import com.nexoia.auth.recovery.provider.PasswordResetDelivery;
import com.nexoia.auth.recovery.repository.PasswordResetTokenRepository;
import com.nexoia.auth.session.model.AuthSession;
import com.nexoia.auth.session.model.SessionStatus;
import com.nexoia.auth.session.repository.AuthSessionRepository;
import com.nexoia.auth.user.model.UserAccount;
import com.nexoia.auth.user.model.UserRole;
import com.nexoia.auth.user.model.UserStatus;
import com.nexoia.auth.user.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class PasswordRecoveryServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-17T01:00:00Z");
    private static final UUID USER_ID = UUID.fromString("a1cb2928-920e-4f62-b134-f8654eb9a28f");

    @Mock private UserAccountRepository userAccountRepository;
    @Mock private PasswordCredentialRepository passwordCredentialRepository;
    @Mock private PasswordResetTokenRepository passwordResetTokenRepository;
    @Mock private AuthSessionRepository authSessionRepository;
    @Mock private PasswordResetTokenService tokenService;
    @Mock private PasswordResetDelivery delivery;
    @Mock private PasswordEncoder passwordEncoder;

    private PasswordRecoveryService service;

    @BeforeEach
    void setUp() {
        service = new PasswordRecoveryService(userAccountRepository, passwordCredentialRepository,
                passwordResetTokenRepository, authSessionRepository, tokenService, delivery,
                passwordEncoder, new PasswordRecoveryProperties(Duration.ofMinutes(20),
                "http://localhost/reset-password", "noreply@nexo.local"),
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void doesNotRevealOrIssueTokenForUnknownEmail() {
        when(userAccountRepository.findByEmailIgnoreCase("missing@nexo.local"))
                .thenReturn(Optional.empty());

        service.request(" missing@nexo.local ", "127.0.0.1");

        verify(passwordResetTokenRepository, never()).save(any());
        verify(delivery, never()).send(any(), any(), any());
    }

    @Test
    void storesOnlyTheHashAndDeliversTheRawToken() {
        UserAccount user = user();
        when(userAccountRepository.findByEmailIgnoreCase("owner@nexo.local"))
                .thenReturn(Optional.of(user));
        when(passwordResetTokenRepository.findAllByUserIdAndUsedAtIsNull(USER_ID))
                .thenReturn(List.of());
        when(tokenService.issue()).thenReturn(new IssuedPasswordResetToken("raw-token", "token-hash"));

        service.request("owner@nexo.local", "127.0.0.1");

        ArgumentCaptor<PasswordResetToken> token = ArgumentCaptor.forClass(PasswordResetToken.class);
        verify(passwordResetTokenRepository).save(token.capture());
        assertThat(token.getValue().getTokenHash()).isEqualTo("token-hash");
        assertThat(token.getValue().getExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(20)));
        verify(delivery).send("owner@nexo.local", "Owner", "raw-token");
    }

    @Test
    void changesPasswordConsumesTokenAndRevokesActiveSessions() {
        PasswordResetToken resetToken = PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .tokenHash("token-hash")
                .requestedIp("127.0.0.1")
                .expiresAt(NOW.plusSeconds(60))
                .build();
        PasswordCredential credential = new PasswordCredential(USER_ID, "old-hash", NOW.minusSeconds(60));
        AuthSession session = AuthSession.builder()
                .id(UUID.randomUUID())
                .userId(USER_ID)
                .status(SessionStatus.ACTIVE)
                .refreshExpiresAt(NOW.plusSeconds(60))
                .build();
        when(tokenService.hash("raw-token")).thenReturn("token-hash");
        when(passwordResetTokenRepository.findByTokenHash("token-hash"))
                .thenReturn(Optional.of(resetToken));
        when(passwordCredentialRepository.findById(USER_ID)).thenReturn(Optional.of(credential));
        when(passwordEncoder.encode("Nexo123!")).thenReturn("new-hash");
        when(passwordResetTokenRepository.findAllByUserIdAndUsedAtIsNull(USER_ID))
                .thenReturn(List.of(resetToken));
        when(authSessionRepository.findAllByUserIdAndStatusOrderByLastSeenAtDesc(
                USER_ID, SessionStatus.ACTIVE)).thenReturn(List.of(session));

        service.reset(new PasswordResetConfirmation("raw-token", "Nexo123!"));

        assertThat(credential.getPasswordHash()).isEqualTo("new-hash");
        assertThat(credential.getChangedAt()).isEqualTo(NOW);
        assertThat(resetToken.getUsedAt()).isEqualTo(NOW);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.REVOKED);
        assertThat(session.getRevokeReason()).isEqualTo("PASSWORD_RESET");
    }

    private UserAccount user() {
        return UserAccount.builder()
                .id(USER_ID)
                .username("owner")
                .email("owner@nexo.local")
                .name("Owner")
                .role(UserRole.OWNER)
                .status(UserStatus.ACTIVE)
                .build();
    }
}
