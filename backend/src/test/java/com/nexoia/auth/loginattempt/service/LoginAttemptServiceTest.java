package com.nexoia.auth.loginattempt.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.auth.loginattempt.config.LoginThrottleProperties;
import com.nexoia.auth.loginattempt.exception.LoginThrottledException;
import com.nexoia.auth.loginattempt.model.LoginAttempt;
import com.nexoia.auth.loginattempt.repository.LoginAttemptRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class LoginAttemptServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-17T02:00:00Z");
    private static final String IP_ADDRESS = "127.0.0.1";

    @Mock private LoginAttemptRepository loginAttemptRepository;

    private LoginAttemptService service;

    @BeforeEach
    void setUp() {
        LoginThrottleProperties properties = new LoginThrottleProperties(
                Duration.ofMinutes(30), 5, Duration.ofSeconds(30),
                7, Duration.ofMinutes(2), 10, Duration.ofMinutes(15));
        service = new LoginAttemptService(loginAttemptRepository, properties,
                Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void allowsLoginBeforeTheFirstThreshold() {
        when(loginAttemptRepository
                .findTop20ByIdentifierHashAndIpAddressAndOccurredAtAfterOrderByOccurredAtDesc(
                        any(), any(), any())).thenReturn(failures(4, NOW.minusSeconds(5)));

        assertThatCode(() -> service.assertAllowed("Owner", IP_ADDRESS))
                .doesNotThrowAnyException();
    }

    @Test
    void blocksDuringTheProgressiveLockPeriod() {
        when(loginAttemptRepository
                .findTop20ByIdentifierHashAndIpAddressAndOccurredAtAfterOrderByOccurredAtDesc(
                        any(), any(), any())).thenReturn(failures(5, NOW.minusSeconds(10)));

        assertThatThrownBy(() -> service.assertAllowed("Owner", IP_ADDRESS))
                .isInstanceOf(LoginThrottledException.class);
    }

    @Test
    void aSuccessfulLoginResetsTheConsecutiveFailureSequence() {
        List<LoginAttempt> history = new java.util.ArrayList<>(failures(2, NOW.minusSeconds(5)));
        history.add(LoginAttempt.builder()
                .successful(true)
                .occurredAt(NOW.minusSeconds(10))
                .build());
        history.addAll(failures(10, NOW.minusSeconds(20)));
        when(loginAttemptRepository
                .findTop20ByIdentifierHashAndIpAddressAndOccurredAtAfterOrderByOccurredAtDesc(
                        any(), any(), any())).thenReturn(history);

        assertThatCode(() -> service.assertAllowed("Owner", IP_ADDRESS))
                .doesNotThrowAnyException();
    }

    @Test
    void storesOnlyAHashOfTheLoginIdentifier() {
        service.recordFailure("Owner@Nexo.Local", IP_ADDRESS);

        ArgumentCaptor<LoginAttempt> attempt = ArgumentCaptor.forClass(LoginAttempt.class);
        verify(loginAttemptRepository).save(attempt.capture());
        assertThat(attempt.getValue().getIdentifierHash()).hasSize(64);
        assertThat(attempt.getValue().getIdentifierHash()).doesNotContain("owner");
        assertThat(attempt.getValue().isSuccessful()).isFalse();
    }

    private List<LoginAttempt> failures(int count, Instant latest) {
        return java.util.stream.IntStream.range(0, count)
                .mapToObj(index -> LoginAttempt.builder()
                        .successful(false)
                        .occurredAt(latest.minusSeconds(index))
                        .build())
                .toList();
    }
}
