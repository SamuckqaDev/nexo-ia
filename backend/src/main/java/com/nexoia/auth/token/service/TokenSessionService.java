package com.nexoia.auth.token.service;

import com.nexoia.auth.access.dto.ClientAccessMetadata;
import com.nexoia.auth.access.model.AccessEvent;
import com.nexoia.auth.access.model.AccessEventType;
import com.nexoia.auth.access.repository.AccessEventRepository;
import com.nexoia.auth.session.model.AuthSession;
import com.nexoia.auth.session.model.SessionStatus;
import com.nexoia.auth.session.repository.AuthSessionRepository;
import com.nexoia.auth.session.security.NexoUserDetailsService;
import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.auth.token.dto.IssuedTokenPair;
import com.nexoia.auth.token.exception.InvalidAccessTokenException;
import com.nexoia.auth.token.exception.InvalidRefreshTokenException;
import com.nexoia.auth.token.exception.RefreshTokenReuseException;
import com.nexoia.auth.token.model.RefreshToken;
import com.nexoia.auth.token.repository.RefreshTokenRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class TokenSessionService {

    private final AuthSessionRepository authSessionRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final AccessEventRepository accessEventRepository;
    private final NexoUserDetailsService userDetailsService;
    private final TokenService tokenService;
    private final Clock clock;

    @Transactional
    public void recordLoginFailure(ClientAccessMetadata metadata) {
        record(null, null, AccessEventType.LOGIN_FAILURE, false, metadata, clock.instant());
    }

    @Transactional
    public IssuedTokenPair start(NexoUserPrincipal principal, ClientAccessMetadata metadata) {
        Instant now = clock.instant();
        UUID sessionId = UUID.randomUUID();
        IssuedTokenPair tokens = tokenService.issue(principal, sessionId);
        authSessionRepository.save(AuthSession.builder()
                .id(sessionId)
                .userId(principal.userId())
                .status(SessionStatus.ACTIVE)
                .currentAccessJti(tokens.accessJti())
                .initialIp(metadata.ipAddress())
                .lastIp(metadata.ipAddress())
                .userAgent(metadata.userAgent())
                .accessExpiresAt(tokens.accessExpiresAt())
                .refreshExpiresAt(tokens.refreshExpiresAt())
                .lastSeenAt(now)
                .build());
        refreshTokenRepository.save(newRefreshToken(sessionId, tokens, metadata, now));
        record(sessionId, principal.userId(), AccessEventType.LOGIN_SUCCESS, true, metadata, now);
        return tokens;
    }

    @Transactional(noRollbackFor = {
            RefreshTokenReuseException.class,
            InvalidRefreshTokenException.class
    })
    public IssuedTokenPair refresh(String rawRefreshToken, ClientAccessMetadata metadata) {
        Instant now = clock.instant();
        RefreshToken current = refreshTokenRepository
                .findByTokenHash(tokenService.hashRefreshToken(rawRefreshToken))
                .orElseThrow(InvalidRefreshTokenException::new);
        AuthSession session = authSessionRepository.findById(current.getSessionId())
                .orElseThrow(InvalidRefreshTokenException::new);

        if (current.getUsedAt() != null) {
            session.revoke(SessionStatus.COMPROMISED, "refresh token reuse", now);
            record(session.getId(), session.getUserId(), AccessEventType.REFRESH_REUSE_DETECTED,
                    false, metadata, now);
            throw new RefreshTokenReuseException();
        }
        if (!current.isUsableAt(now) || !session.isActiveAt(now)) {
            session.revoke(SessionStatus.REVOKED, "refresh token expired or invalid", now);
            throw new InvalidRefreshTokenException();
        }

        NexoUserPrincipal principal = userDetailsService.loadUserById(session.getUserId());
        IssuedTokenPair next = tokenService.issue(principal, session.getId());
        UUID nextTokenId = UUID.randomUUID();
        current.rotateTo(nextTokenId, now);
        refreshTokenRepository.save(RefreshToken.builder()
                .id(nextTokenId)
                .sessionId(session.getId())
                .tokenHash(next.refreshTokenHash())
                .issuedIp(metadata.ipAddress())
                .issuedAt(now)
                .expiresAt(next.refreshExpiresAt())
                .build());
        session.rotateAccess(next.accessJti(), next.accessExpiresAt(), metadata.ipAddress(), now);
        record(session.getId(), session.getUserId(), AccessEventType.TOKEN_REFRESH, true, metadata, now);
        return next;
    }

    @Transactional
    public NexoUserPrincipal authenticate(String rawAccessToken, ClientAccessMetadata metadata) {
        Instant now = clock.instant();
        try {
            Jwt jwt = tokenService.decode(rawAccessToken);
            UUID sessionId = UUID.fromString(jwt.getClaimAsString("sid"));
            UUID accessJti = UUID.fromString(jwt.getId());
            AuthSession session = authSessionRepository.findById(sessionId)
                    .orElseThrow(InvalidAccessTokenException::new);
            if (!session.isActiveAt(now) || !session.getCurrentAccessJti().equals(accessJti)
                    || !session.getAccessExpiresAt().isAfter(now)
                    || !session.getUserId().toString().equals(jwt.getSubject())) {
                throw new InvalidAccessTokenException();
            }
            NexoUserPrincipal principal = userDetailsService.loadUserById(session.getUserId());
            if (!principal.enabled()) {
                throw new InvalidAccessTokenException();
            }
            session.recordAccess(metadata.ipAddress(), now);
            record(sessionId, session.getUserId(), AccessEventType.ACCESS_GRANTED, true, metadata, now);
            return principal;
        } catch (JwtException | IllegalArgumentException exception) {
            throw new InvalidAccessTokenException();
        }
    }

    @Transactional
    public void revokeAccessToken(String rawAccessToken, ClientAccessMetadata metadata) {
        Instant now = clock.instant();
        try {
            Jwt jwt = tokenService.decode(rawAccessToken);
            UUID sessionId = UUID.fromString(jwt.getClaimAsString("sid"));
            authSessionRepository.findById(sessionId).ifPresent(session -> {
                session.revoke(SessionStatus.REVOKED, "user logout", now);
                record(sessionId, session.getUserId(), AccessEventType.LOGOUT, true, metadata, now);
            });
        } catch (JwtException | IllegalArgumentException ignored) {
            // Clearing cookies is safe and idempotent when the access token is already invalid.
        }
    }

    public UUID sessionId(String rawAccessToken) {
        try {
            return UUID.fromString(tokenService.decode(rawAccessToken).getClaimAsString("sid"));
        } catch (JwtException | IllegalArgumentException exception) {
            throw new InvalidAccessTokenException();
        }
    }

    private RefreshToken newRefreshToken(UUID sessionId, IssuedTokenPair tokens,
            ClientAccessMetadata metadata, Instant issuedAt) {
        return RefreshToken.builder()
                .id(UUID.randomUUID())
                .sessionId(sessionId)
                .tokenHash(tokens.refreshTokenHash())
                .issuedIp(metadata.ipAddress())
                .issuedAt(issuedAt)
                .expiresAt(tokens.refreshExpiresAt())
                .build();
    }

    private void record(UUID sessionId, UUID userId, AccessEventType eventType, boolean success,
            ClientAccessMetadata metadata, Instant occurredAt) {
        accessEventRepository.save(AccessEvent.builder()
                .sessionId(sessionId)
                .userId(userId)
                .eventType(eventType)
                .success(success)
                .ipAddress(metadata.ipAddress())
                .userAgent(metadata.userAgent())
                .occurredAt(occurredAt)
                .build());
    }
}
