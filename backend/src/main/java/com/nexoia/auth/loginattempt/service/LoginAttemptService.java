package com.nexoia.auth.loginattempt.service;

import com.nexoia.auth.loginattempt.config.LoginThrottleProperties;
import com.nexoia.auth.loginattempt.exception.LoginAttemptHashException;
import com.nexoia.auth.loginattempt.exception.LoginThrottledException;
import com.nexoia.auth.loginattempt.model.LoginAttempt;
import com.nexoia.auth.loginattempt.repository.LoginAttemptRepository;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.HexFormat;
import java.util.List;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class LoginAttemptService {

    private final LoginAttemptRepository loginAttemptRepository;
    private final LoginThrottleProperties properties;
    private final Clock clock;

    @Transactional(readOnly = true)
    public void assertAllowed(String identifier, String ipAddress) {
        Instant now = clock.instant();
        List<LoginAttempt> history = recentHistory(identifier, ipAddress, now);
        int consecutiveFailures = 0;
        Instant latestFailure = null;

        for (LoginAttempt attempt : history) {
            if (attempt.isSuccessful()) {
                break;
            }
            if (latestFailure == null) {
                latestFailure = attempt.getOccurredAt();
            }
            consecutiveFailures++;
        }

        Duration lockDuration = lockDuration(consecutiveFailures);
        if (latestFailure != null && !lockDuration.isZero()
                && latestFailure.plus(lockDuration).isAfter(now)) {
            throw new LoginThrottledException();
        }
    }

    @Transactional
    public void recordFailure(String identifier, String ipAddress) {
        record(identifier, ipAddress, false);
    }

    @Transactional
    public void recordSuccess(String identifier, String ipAddress) {
        record(identifier, ipAddress, true);
    }

    private List<LoginAttempt> recentHistory(String identifier, String ipAddress, Instant now) {
        return loginAttemptRepository
                .findTop20ByIdentifierHashAndIpAddressAndOccurredAtAfterOrderByOccurredAtDesc(
                        hash(identifier), ipAddress, now.minus(properties.observationWindow()));
    }

    private Duration lockDuration(int consecutiveFailures) {
        if (consecutiveFailures >= properties.maximumThreshold()) {
            return properties.maximumLock();
        }
        if (consecutiveFailures >= properties.secondThreshold()) {
            return properties.secondLock();
        }
        if (consecutiveFailures >= properties.firstThreshold()) {
            return properties.firstLock();
        }
        return Duration.ZERO;
    }

    private void record(String identifier, String ipAddress, boolean successful) {
        loginAttemptRepository.save(LoginAttempt.builder()
                .identifierHash(hash(identifier))
                .ipAddress(ipAddress)
                .successful(successful)
                .occurredAt(clock.instant())
                .build());
    }

    private String hash(String identifier) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] value = identifier.trim().toLowerCase(Locale.ROOT)
                    .getBytes(StandardCharsets.UTF_8);
            return HexFormat.of().formatHex(digest.digest(value));
        } catch (NoSuchAlgorithmException exception) {
            throw new LoginAttemptHashException(exception);
        }
    }
}
