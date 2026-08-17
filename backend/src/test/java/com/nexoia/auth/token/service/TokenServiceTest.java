package com.nexoia.auth.token.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.auth.token.config.TokenProperties;
import com.nexoia.auth.token.exception.InvalidTokenConfigurationException;
import com.nexoia.auth.user.model.UserRole;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class TokenServiceTest {

    private static final Instant NOW = Instant.now();

    @Test
    void issuesSignedShortLivedAccessJwtAndOpaqueRefreshToken() {
        TokenService service = serviceWithSecret(new byte[32]);
        UUID userId = UUID.randomUUID();
        UUID sessionId = UUID.randomUUID();
        var principal = new NexoUserPrincipal(userId, "owner", "owner@nexo.local", "Owner", NOW,
                UserRole.OWNER, "encoded", true);

        var pair = service.issue(principal, sessionId);
        var jwt = service.decode(pair.accessToken());

        assertThat(jwt.getSubject()).isEqualTo(userId.toString());
        assertThat(jwt.getClaimAsString("sid")).isEqualTo(sessionId.toString());
        assertThat(jwt.getId()).isEqualTo(pair.accessJti().toString());
        assertThat(pair.accessExpiresAt()).isEqualTo(NOW.plus(Duration.ofMinutes(5)));
        assertThat(pair.refreshExpiresAt()).isEqualTo(NOW.plus(Duration.ofDays(30)));
        assertThat(pair.refreshToken()).doesNotContain(".");
        assertThat(pair.refreshTokenHash()).hasSize(64).isNotEqualTo(pair.refreshToken());
    }

    @Test
    void rejectsSigningSecretsShorterThan256Bits() {
        assertThatThrownBy(() -> serviceWithSecret(new byte[16]))
                .isInstanceOf(InvalidTokenConfigurationException.class)
                .hasMessageContaining("at least 32 random bytes");
    }

    private TokenService serviceWithSecret(byte[] secret) {
        var properties = new TokenProperties("nexo-ia", Base64.getEncoder().encodeToString(secret),
                Duration.ofMinutes(5), Duration.ofDays(30), false);
        return new TokenService(properties, Clock.fixed(NOW, ZoneOffset.UTC));
    }
}
