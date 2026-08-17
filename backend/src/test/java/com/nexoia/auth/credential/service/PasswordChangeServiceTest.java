package com.nexoia.auth.credential.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.auth.access.dto.ClientAccessMetadata;
import com.nexoia.auth.access.model.AccessEvent;
import com.nexoia.auth.access.model.AccessEventType;
import com.nexoia.auth.access.repository.AccessEventRepository;
import com.nexoia.auth.access.service.ClientAccessService;
import com.nexoia.auth.credential.dto.ChangePasswordRequest;
import com.nexoia.auth.credential.exception.CurrentPasswordIncorrectException;
import com.nexoia.auth.credential.exception.PasswordReuseException;
import com.nexoia.auth.credential.model.PasswordCredential;
import com.nexoia.auth.credential.repository.PasswordCredentialRepository;
import com.nexoia.auth.session.model.AuthSession;
import com.nexoia.auth.session.model.SessionStatus;
import com.nexoia.auth.session.repository.AuthSessionRepository;
import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.auth.token.service.TokenCookieService;
import com.nexoia.auth.token.service.TokenSessionService;
import com.nexoia.auth.user.model.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
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
class PasswordChangeServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-17T02:00:00Z");
    private static final UUID USER_ID = UUID.fromString("a1cb2928-920e-4f62-b134-f8654eb9a28f");
    private static final UUID CURRENT_SESSION_ID = UUID.fromString("92a7c75b-d392-4e48-a62f-09ef72416824");

    @Mock private PasswordCredentialRepository passwordCredentialRepository;
    @Mock private AuthSessionRepository authSessionRepository;
    @Mock private AccessEventRepository accessEventRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private TokenCookieService tokenCookieService;
    @Mock private TokenSessionService tokenSessionService;
    @Mock private ClientAccessService clientAccessService;
    @Mock private HttpServletRequest httpRequest;

    private PasswordChangeService service;

    @BeforeEach
    void setUp() {
        service = new PasswordChangeService(passwordCredentialRepository, authSessionRepository,
                accessEventRepository, passwordEncoder, tokenCookieService, tokenSessionService,
                clientAccessService, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void rejectsAnIncorrectCurrentPassword() {
        PasswordCredential credential = credential();
        when(passwordCredentialRepository.findById(USER_ID)).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("wrong", "old-hash")).thenReturn(false);

        assertThatThrownBy(() -> service.change(principal(),
                new ChangePasswordRequest("wrong", "Nexo456!"), httpRequest))
                .isInstanceOf(CurrentPasswordIncorrectException.class);

        verify(passwordEncoder, never()).encode(any());
        verify(accessEventRepository, never()).save(any());
    }

    @Test
    void rejectsReuseOfTheCurrentPassword() {
        PasswordCredential credential = credential();
        when(passwordCredentialRepository.findById(USER_ID)).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("Nexo123!", "old-hash")).thenReturn(true);

        assertThatThrownBy(() -> service.change(principal(),
                new ChangePasswordRequest("Nexo123!", "Nexo123!"), httpRequest))
                .isInstanceOf(PasswordReuseException.class);

        verify(passwordEncoder, never()).encode(any());
    }

    @Test
    void changesPasswordKeepsCurrentSessionAndRevokesTheOthers() {
        PasswordCredential credential = credential();
        AuthSession currentSession = session(CURRENT_SESSION_ID);
        AuthSession otherSession = session(UUID.randomUUID());
        ClientAccessMetadata metadata = new ClientAccessMetadata("127.0.0.1", "Nexo test");
        when(passwordCredentialRepository.findById(USER_ID)).thenReturn(Optional.of(credential));
        when(passwordEncoder.matches("Nexo123!", "old-hash")).thenReturn(true);
        when(passwordEncoder.matches("Nexo456!", "old-hash")).thenReturn(false);
        when(tokenCookieService.accessToken(httpRequest)).thenReturn(Optional.of("access-token"));
        when(tokenSessionService.sessionId("access-token")).thenReturn(CURRENT_SESSION_ID);
        when(passwordEncoder.encode("Nexo456!")).thenReturn("new-hash");
        when(authSessionRepository.findAllByUserIdAndStatusOrderByLastSeenAtDesc(
                USER_ID, SessionStatus.ACTIVE)).thenReturn(List.of(currentSession, otherSession));
        when(clientAccessService.extract(httpRequest)).thenReturn(metadata);

        service.change(principal(),
                new ChangePasswordRequest("Nexo123!", "Nexo456!"), httpRequest);

        assertThat(credential.getPasswordHash()).isEqualTo("new-hash");
        assertThat(credential.getChangedAt()).isEqualTo(NOW);
        assertThat(currentSession.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(otherSession.getStatus()).isEqualTo(SessionStatus.REVOKED);
        assertThat(otherSession.getRevokeReason()).isEqualTo("PASSWORD_CHANGED_FROM_ANOTHER_SESSION");

        ArgumentCaptor<AccessEvent> event = ArgumentCaptor.forClass(AccessEvent.class);
        verify(accessEventRepository).save(event.capture());
        assertThat(event.getValue().getEventType()).isEqualTo(AccessEventType.PASSWORD_CHANGED);
        assertThat(event.getValue().getSessionId()).isEqualTo(CURRENT_SESSION_ID);
        assertThat(event.getValue().isSuccess()).isTrue();
    }

    private PasswordCredential credential() {
        return new PasswordCredential(USER_ID, "old-hash", NOW.minusSeconds(60));
    }

    private AuthSession session(UUID id) {
        return AuthSession.builder()
                .id(id)
                .userId(USER_ID)
                .status(SessionStatus.ACTIVE)
                .refreshExpiresAt(NOW.plusSeconds(300))
                .build();
    }

    private NexoUserPrincipal principal() {
        return new NexoUserPrincipal(USER_ID, "owner", "owner@nexo.local", "Owner", NOW,
                UserRole.OWNER, "old-hash", true);
    }
}
