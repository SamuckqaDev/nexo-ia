package com.nexoia.auth.recovery.service;

import com.nexoia.auth.credential.model.PasswordCredential;
import com.nexoia.auth.credential.repository.PasswordCredentialRepository;
import com.nexoia.auth.recovery.config.PasswordRecoveryProperties;
import com.nexoia.auth.recovery.dto.IssuedPasswordResetToken;
import com.nexoia.auth.recovery.dto.PasswordResetConfirmation;
import com.nexoia.auth.recovery.exception.InvalidPasswordResetTokenException;
import com.nexoia.auth.recovery.exception.PasswordResetDeliveryException;
import com.nexoia.auth.recovery.model.PasswordResetToken;
import com.nexoia.auth.recovery.provider.PasswordResetDelivery;
import com.nexoia.auth.recovery.repository.PasswordResetTokenRepository;
import com.nexoia.auth.session.model.AuthSession;
import com.nexoia.auth.session.model.SessionStatus;
import com.nexoia.auth.session.repository.AuthSessionRepository;
import com.nexoia.auth.user.model.UserAccount;
import com.nexoia.auth.user.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class PasswordRecoveryService {

    private static final String RESET_REVOKE_REASON = "PASSWORD_RESET";

    private final UserAccountRepository userAccountRepository;
    private final PasswordCredentialRepository passwordCredentialRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final AuthSessionRepository authSessionRepository;
    private final PasswordResetTokenService tokenService;
    private final PasswordResetDelivery delivery;
    private final PasswordEncoder passwordEncoder;
    private final PasswordRecoveryProperties properties;
    private final Clock clock;

    @Transactional
    public void request(String email, String requestedIp) {
        userAccountRepository.findByEmailIgnoreCase(email.trim().toLowerCase(Locale.ROOT))
                .ifPresent(user -> issueFor(user, requestedIp));
    }

    @Transactional
    public void reset(PasswordResetConfirmation request) {
        Instant now = clock.instant();
        PasswordResetToken resetToken = passwordResetTokenRepository
                .findByTokenHash(tokenService.hash(request.token()))
                .filter(token -> token.isUsableAt(now))
                .orElseThrow(InvalidPasswordResetTokenException::new);
        PasswordCredential credential = passwordCredentialRepository.findById(resetToken.getUserId())
                .orElseThrow(InvalidPasswordResetTokenException::new);

        credential.changePassword(passwordEncoder.encode(request.password()), now);
        resetToken.markUsed(now);
        invalidateUnusedTokens(resetToken.getUserId(), now);
        revokeActiveSessions(resetToken.getUserId(), now);
    }

    private void issueFor(UserAccount user, String requestedIp) {
        Instant now = clock.instant();
        invalidateUnusedTokens(user.getId(), now);
        IssuedPasswordResetToken issued = tokenService.issue();
        passwordResetTokenRepository.save(PasswordResetToken.builder()
                .id(UUID.randomUUID())
                .userId(user.getId())
                .tokenHash(issued.hash())
                .requestedIp(requestedIp)
                .expiresAt(now.plus(properties.tokenTtl()))
                .build());

        try {
            delivery.send(user.getEmail(), user.getName(), issued.value());
        } catch (PasswordResetDeliveryException exception) {
            log.error("Password reset delivery failed for user {}", user.getId(), exception);
        }
    }

    private void invalidateUnusedTokens(UUID userId, Instant now) {
        passwordResetTokenRepository.findAllByUserIdAndUsedAtIsNull(userId)
                .forEach(token -> token.markUsed(now));
    }

    private void revokeActiveSessions(UUID userId, Instant now) {
        authSessionRepository.findAllByUserIdAndStatusOrderByLastSeenAtDesc(userId, SessionStatus.ACTIVE)
                .forEach(session -> session.revoke(SessionStatus.REVOKED, RESET_REVOKE_REASON, now));
    }
}
