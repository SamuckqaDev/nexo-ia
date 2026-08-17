package com.nexoia.auth.user.service;

import com.nexoia.auth.access.dto.ClientAccessMetadata;
import com.nexoia.auth.access.model.AccessEvent;
import com.nexoia.auth.access.model.AccessEventType;
import com.nexoia.auth.access.repository.AccessEventRepository;
import com.nexoia.auth.access.service.ClientAccessService;
import com.nexoia.auth.credential.model.PasswordCredential;
import com.nexoia.auth.credential.repository.PasswordCredentialRepository;
import com.nexoia.auth.session.model.SessionStatus;
import com.nexoia.auth.session.model.AuthSession;
import com.nexoia.auth.session.dto.SessionResponse;
import com.nexoia.auth.session.exception.SessionNotFoundException;
import com.nexoia.auth.session.repository.AuthSessionRepository;
import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.auth.token.exception.InvalidAccessTokenException;
import com.nexoia.auth.token.service.TokenCookieService;
import com.nexoia.auth.token.service.TokenSessionService;
import com.nexoia.auth.user.dto.CreateMemberRequest;
import com.nexoia.auth.user.dto.ManagedUserResponse;
import com.nexoia.auth.user.dto.UpdateUserStatusRequest;
import com.nexoia.auth.user.exception.OwnerStatusChangeException;
import com.nexoia.auth.user.exception.MemberAdministrationException;
import com.nexoia.auth.user.exception.UserIdentityAlreadyExistsException;
import com.nexoia.auth.user.exception.UserNotFoundException;
import com.nexoia.auth.user.model.UserAccount;
import com.nexoia.auth.user.model.UserRole;
import com.nexoia.auth.user.model.UserStatus;
import com.nexoia.auth.user.repository.UserAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserManagementService {

    private static final String USER_DISABLED_REASON = "USER_DISABLED_BY_OWNER";
    private static final String ADMIN_SESSION_REVOCATION_REASON = "SESSION_REVOKED_BY_OWNER";

    private final UserAccountRepository userAccountRepository;
    private final PasswordCredentialRepository passwordCredentialRepository;
    private final AuthSessionRepository authSessionRepository;
    private final AccessEventRepository accessEventRepository;
    private final PasswordEncoder passwordEncoder;
    private final TokenCookieService tokenCookieService;
    private final TokenSessionService tokenSessionService;
    private final ClientAccessService clientAccessService;
    private final Clock clock;

    @Transactional(readOnly = true)
    public List<ManagedUserResponse> list() {
        return userAccountRepository.findAllByOrderByCreatedAtAsc().stream()
                .map(this::toResponse)
                .toList();
    }

    @Transactional
    public ManagedUserResponse createMember(NexoUserPrincipal principal, CreateMemberRequest request,
            HttpServletRequest httpRequest) {
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        assertIdentityAvailable(username, email);

        UUID userId = UUID.randomUUID();
        Instant now = clock.instant();
        UserAccount member = UserAccount.builder()
                .id(userId)
                .username(username)
                .email(email)
                .name(request.name().trim())
                .role(UserRole.MEMBER)
                .status(UserStatus.ACTIVE)
                .build();
        try {
            member = userAccountRepository.saveAndFlush(member);
            passwordCredentialRepository.save(new PasswordCredential(
                    userId, passwordEncoder.encode(request.password()), now));
        } catch (DataIntegrityViolationException exception) {
            throw new UserIdentityAlreadyExistsException();
        }
        record(principal, AccessEventType.USER_CREATED, httpRequest, now);
        return toResponse(member);
    }

    @Transactional
    public ManagedUserResponse changeStatus(NexoUserPrincipal principal, UUID userId,
            UpdateUserStatusRequest request, HttpServletRequest httpRequest) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        if (user.getRole() == UserRole.OWNER) {
            throw new OwnerStatusChangeException();
        }

        Instant now = clock.instant();
        user.changeStatus(request.status());
        if (request.status() == UserStatus.DISABLED) {
            authSessionRepository.findAllByUserIdAndStatusOrderByLastSeenAtDesc(
                    userId, SessionStatus.ACTIVE)
                    .forEach(session -> session.revoke(
                            SessionStatus.REVOKED, USER_DISABLED_REASON, now));
        }
        userAccountRepository.saveAndFlush(user);
        record(principal, AccessEventType.USER_STATUS_CHANGED, httpRequest, now);
        return toResponse(user);
    }

    @Transactional(readOnly = true)
    public List<SessionResponse> activeSessions(UUID userId) {
        requireMember(userId);
        return authSessionRepository.findAllByUserIdAndStatusOrderByLastSeenAtDesc(
                userId, SessionStatus.ACTIVE).stream()
                .map(session -> new SessionResponse(session.getId(), session.getStatus(),
                        session.getInitialIp(), session.getLastIp(), session.getUserAgent(),
                        session.getCreatedAt(), session.getLastSeenAt(), session.getAccessExpiresAt(),
                        session.getRefreshExpiresAt(), false))
                .toList();
    }

    @Transactional
    public void revokeSession(NexoUserPrincipal principal, UUID userId, UUID sessionId,
            HttpServletRequest httpRequest) {
        requireMember(userId);
        AuthSession session = authSessionRepository.findByIdAndUserIdAndStatus(
                sessionId, userId, SessionStatus.ACTIVE)
                .orElseThrow(SessionNotFoundException::new);
        Instant now = clock.instant();
        session.revoke(SessionStatus.REVOKED, ADMIN_SESSION_REVOCATION_REASON, now);
        record(principal, AccessEventType.ADMIN_SESSION_REVOKED, httpRequest, now);
    }

    private UserAccount requireMember(UUID userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        if (user.getRole() != UserRole.MEMBER) {
            throw new MemberAdministrationException();
        }
        return user;
    }

    private void assertIdentityAvailable(String username, String email) {
        if (userAccountRepository.existsByUsernameIgnoreCase(username)
                || userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw new UserIdentityAlreadyExistsException();
        }
    }

    private void record(NexoUserPrincipal principal, AccessEventType eventType,
            HttpServletRequest request, Instant now) {
        UUID sessionId = tokenCookieService.accessToken(request)
                .map(tokenSessionService::sessionId)
                .orElseThrow(InvalidAccessTokenException::new);
        ClientAccessMetadata metadata = clientAccessService.extract(request);
        accessEventRepository.save(AccessEvent.builder()
                .sessionId(sessionId)
                .userId(principal.userId())
                .eventType(eventType)
                .success(true)
                .ipAddress(metadata.ipAddress())
                .userAgent(metadata.userAgent())
                .occurredAt(now)
                .build());
    }

    private ManagedUserResponse toResponse(UserAccount user) {
        return new ManagedUserResponse(user.getId(), user.getUsername(), user.getEmail(),
                user.getName(), user.getRole(), user.getStatus(), user.getCreatedAt(),
                user.getUpdatedAt());
    }
}
