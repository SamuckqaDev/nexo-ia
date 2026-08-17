package com.nexoia.auth.session.service;

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
import com.nexoia.auth.token.exception.InvalidAccessTokenException;
import com.nexoia.auth.token.service.TokenCookieService;
import com.nexoia.auth.token.service.TokenSessionService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class SessionManagementService {

    private static final String REMOTE_REVOCATION_REASON = "REMOTE_USER_REVOCATION";
    private static final String OTHER_SESSIONS_REVOCATION_REASON = "USER_REVOKED_OTHER_SESSIONS";

    private final AuthSessionRepository authSessionRepository;
    private final AccessEventRepository accessEventRepository;
    private final TokenCookieService tokenCookieService;
    private final TokenSessionService tokenSessionService;
    private final ClientAccessService clientAccessService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<SessionResponse> activeSessions(NexoUserPrincipal principal,
            HttpServletRequest request) {
        UUID currentSessionId = currentSessionId(request);
        return authSessionRepository
                .findAllByUserIdAndStatusOrderByLastSeenAtDesc(
                        principal.userId(), SessionStatus.ACTIVE)
                .stream()
                .map(session -> toResponse(session, currentSessionId))
                .toList();
    }

    @Transactional
    public void revoke(NexoUserPrincipal principal, UUID sessionId,
            HttpServletRequest request) {
        UUID currentSessionId = currentSessionId(request);
        if (currentSessionId.equals(sessionId)) {
            throw new CurrentSessionRevocationException();
        }

        AuthSession session = authSessionRepository
                .findByIdAndUserIdAndStatus(sessionId, principal.userId(), SessionStatus.ACTIVE)
                .orElseThrow(SessionNotFoundException::new);
        Instant now = clock.instant();
        session.revoke(SessionStatus.REVOKED, REMOTE_REVOCATION_REASON, now);
        recordRevocation(principal.userId(), currentSessionId,
                clientAccessService.extract(request), now);
    }

    @Transactional
    public void revokeOthers(NexoUserPrincipal principal, HttpServletRequest request) {
        UUID currentSessionId = currentSessionId(request);
        Instant now = clock.instant();
        authSessionRepository.findAllByUserIdAndStatusOrderByLastSeenAtDesc(
                principal.userId(), SessionStatus.ACTIVE).stream()
                .filter(session -> !session.getId().equals(currentSessionId))
                .forEach(session -> session.revoke(SessionStatus.REVOKED,
                        OTHER_SESSIONS_REVOCATION_REASON, now));
        recordRevocation(principal.userId(), currentSessionId,
                clientAccessService.extract(request), now);
    }

    private UUID currentSessionId(HttpServletRequest request) {
        return tokenCookieService.accessToken(request)
                .map(tokenSessionService::sessionId)
                .orElseThrow(InvalidAccessTokenException::new);
    }

    private void recordRevocation(UUID userId, UUID currentSessionId,
            ClientAccessMetadata metadata, Instant now) {
        accessEventRepository.save(AccessEvent.builder()
                .sessionId(currentSessionId)
                .userId(userId)
                .eventType(AccessEventType.SESSION_REVOKED)
                .success(true)
                .ipAddress(metadata.ipAddress())
                .userAgent(metadata.userAgent())
                .occurredAt(now)
                .build());
    }

    private SessionResponse toResponse(AuthSession session, UUID currentSessionId) {
        return new SessionResponse(session.getId(), session.getStatus(), session.getInitialIp(),
                session.getLastIp(), session.getUserAgent(), session.getCreatedAt(),
                session.getLastSeenAt(), session.getAccessExpiresAt(),
                session.getRefreshExpiresAt(), session.getId().equals(currentSessionId));
    }
}
