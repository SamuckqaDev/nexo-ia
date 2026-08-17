package com.nexoia.auth.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.auth.access.dto.ClientAccessMetadata;
import com.nexoia.auth.access.model.AccessEvent;
import com.nexoia.auth.access.model.AccessEventType;
import com.nexoia.auth.access.repository.AccessEventRepository;
import com.nexoia.auth.access.service.ClientAccessService;
import com.nexoia.auth.session.dto.SessionResponse;
import com.nexoia.auth.session.exception.CurrentSessionRevocationException;
import com.nexoia.auth.session.exception.SessionNotFoundException;
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

@ExtendWith(MockitoExtension.class)
class SessionManagementServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-17T02:00:00Z");
    private static final UUID USER_ID = UUID.fromString("a1cb2928-920e-4f62-b134-f8654eb9a28f");
    private static final UUID CURRENT_ID = UUID.fromString("92a7c75b-d392-4e48-a62f-09ef72416824");
    private static final UUID OTHER_ID = UUID.fromString("afda2928-920e-4f62-b134-f8654eb9a28f");

    @Mock private AuthSessionRepository authSessionRepository;
    @Mock private AccessEventRepository accessEventRepository;
    @Mock private TokenCookieService tokenCookieService;
    @Mock private TokenSessionService tokenSessionService;
    @Mock private ClientAccessService clientAccessService;
    @Mock private HttpServletRequest request;

    private SessionManagementService service;

    @BeforeEach
    void setUp() {
        service = new SessionManagementService(authSessionRepository, accessEventRepository,
                tokenCookieService, tokenSessionService, clientAccessService,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void identifiesTheCurrentSessionInTheActiveSessionList() {
        authenticateCurrentRequest();
        when(authSessionRepository.findAllByUserIdAndStatusOrderByLastSeenAtDesc(
                USER_ID, SessionStatus.ACTIVE)).thenReturn(List.of(session(CURRENT_ID), session(OTHER_ID)));

        List<SessionResponse> result = service.activeSessions(principal(), request);

        assertThat(result).hasSize(2);
        assertThat(result).filteredOn(SessionResponse::current)
                .extracting(SessionResponse::id)
                .containsExactly(CURRENT_ID);
    }

    @Test
    void revokesAnotherOwnedSessionAndRecordsTheAction() {
        authenticateCurrentRequest();
        AuthSession otherSession = session(OTHER_ID);
        when(authSessionRepository.findByIdAndUserIdAndStatus(
                OTHER_ID, USER_ID, SessionStatus.ACTIVE)).thenReturn(Optional.of(otherSession));
        when(clientAccessService.extract(request))
                .thenReturn(new ClientAccessMetadata("127.0.0.1", "Nexo test"));

        service.revoke(principal(), OTHER_ID, request);

        assertThat(otherSession.getStatus()).isEqualTo(SessionStatus.REVOKED);
        assertThat(otherSession.getRevokeReason()).isEqualTo("REMOTE_USER_REVOCATION");
        ArgumentCaptor<AccessEvent> event = ArgumentCaptor.forClass(AccessEvent.class);
        verify(accessEventRepository).save(event.capture());
        assertThat(event.getValue().getEventType()).isEqualTo(AccessEventType.SESSION_REVOKED);
        assertThat(event.getValue().getSessionId()).isEqualTo(CURRENT_ID);
    }

    @Test
    void requiresLogoutToEndTheCurrentSession() {
        authenticateCurrentRequest();

        assertThatThrownBy(() -> service.revoke(principal(), CURRENT_ID, request))
                .isInstanceOf(CurrentSessionRevocationException.class);

        verify(authSessionRepository, never())
                .findByIdAndUserIdAndStatus(CURRENT_ID, USER_ID, SessionStatus.ACTIVE);
    }

    @Test
    void doesNotRevealSessionsNotOwnedByTheCurrentUser() {
        authenticateCurrentRequest();
        when(authSessionRepository.findByIdAndUserIdAndStatus(
                OTHER_ID, USER_ID, SessionStatus.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.revoke(principal(), OTHER_ID, request))
                .isInstanceOf(SessionNotFoundException.class);
    }

    @Test
    void revokesAllOtherSessionsButKeepsTheCurrentOne() {
        authenticateCurrentRequest();
        AuthSession current = session(CURRENT_ID);
        AuthSession other = session(OTHER_ID);
        when(authSessionRepository.findAllByUserIdAndStatusOrderByLastSeenAtDesc(
                USER_ID, SessionStatus.ACTIVE)).thenReturn(List.of(current, other));
        when(clientAccessService.extract(request))
                .thenReturn(new ClientAccessMetadata("127.0.0.1", "Nexo test"));

        service.revokeOthers(principal(), request);

        assertThat(current.getStatus()).isEqualTo(SessionStatus.ACTIVE);
        assertThat(other.getStatus()).isEqualTo(SessionStatus.REVOKED);
        assertThat(other.getRevokeReason()).isEqualTo("USER_REVOKED_OTHER_SESSIONS");
    }

    private void authenticateCurrentRequest() {
        when(tokenCookieService.accessToken(request)).thenReturn(Optional.of("access-token"));
        when(tokenSessionService.sessionId("access-token")).thenReturn(CURRENT_ID);
    }

    private AuthSession session(UUID id) {
        return AuthSession.builder()
                .id(id)
                .userId(USER_ID)
                .status(SessionStatus.ACTIVE)
                .initialIp("127.0.0.1")
                .lastIp("127.0.0.1")
                .userAgent("Nexo test")
                .createdAt(NOW.minusSeconds(120))
                .lastSeenAt(NOW.minusSeconds(30))
                .accessExpiresAt(NOW.plusSeconds(300))
                .refreshExpiresAt(NOW.plusSeconds(3600))
                .build();
    }

    private NexoUserPrincipal principal() {
        return new NexoUserPrincipal(USER_ID, "owner", "owner@nexo.local", "Owner", NOW,
                UserRole.OWNER, "password-hash", true);
    }
}
