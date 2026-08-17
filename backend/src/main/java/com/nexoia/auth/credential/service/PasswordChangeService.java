package com.nexoia.auth.credential.service;

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
import com.nexoia.auth.session.exception.UnauthenticatedSessionException;
import com.nexoia.auth.session.model.SessionStatus;
import com.nexoia.auth.session.repository.AuthSessionRepository;
import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.auth.token.exception.InvalidAccessTokenException;
import com.nexoia.auth.token.service.TokenCookieService;
import com.nexoia.auth.token.service.TokenSessionService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class PasswordChangeService {

    private static final String OTHER_SESSION_REVOKE_REASON = "PASSWORD_CHANGED_FROM_ANOTHER_SESSION";

    private final PasswordCredentialRepository passwordCredentialRepository;
    private final AuthSessionRepository authSessionRepository;
    private final AccessEventRepository accessEventRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenCookieService tokenCookieService;
    private final TokenSessionService tokenSessionService;
    private final ClientAccessService clientAccessService;
    private final Clock clock;

    @Transactional
    public void change(NexoUserPrincipal principal, ChangePasswordRequest request,
            HttpServletRequest httpRequest) {
        Instant now = clock.instant();
        PasswordCredential credential = passwordCredentialRepository.findById(principal.userId())
                .orElseThrow(UnauthenticatedSessionException::new);

        if (!passwordEncoder.matches(request.currentPassword(), credential.getPasswordHash())) {
            throw new CurrentPasswordIncorrectException();
        }
        if (passwordEncoder.matches(request.newPassword(), credential.getPasswordHash())) {
            throw new PasswordReuseException();
        }

        UUID currentSessionId = tokenCookieService.accessToken(httpRequest)
                .map(tokenSessionService::sessionId)
                .orElseThrow(InvalidAccessTokenException::new);
        credential.changePassword(passwordEncoder.encode(request.newPassword()), now);
        revokeOtherSessions(principal.userId(), currentSessionId, now);
        recordPasswordChange(principal.userId(), currentSessionId,
                clientAccessService.extract(httpRequest), now);
    }

    private void revokeOtherSessions(UUID userId, UUID currentSessionId, Instant now) {
        authSessionRepository.findAllByUserIdAndStatusOrderByLastSeenAtDesc(userId, SessionStatus.ACTIVE)
                .stream()
                .filter(session -> !session.getId().equals(currentSessionId))
                .forEach(session -> session.revoke(
                        SessionStatus.REVOKED, OTHER_SESSION_REVOKE_REASON, now));
    }

    private void recordPasswordChange(UUID userId, UUID sessionId,
            ClientAccessMetadata metadata, Instant now) {
        accessEventRepository.save(AccessEvent.builder()
                .sessionId(sessionId)
                .userId(userId)
                .eventType(AccessEventType.PASSWORD_CHANGED)
                .success(true)
                .ipAddress(metadata.ipAddress())
                .userAgent(metadata.userAgent())
                .occurredAt(now)
                .build());
    }
}
