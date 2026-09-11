package com.nexoia.provider.secret.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexoia.provider.secret.config.ProviderSecretProperties;
import com.nexoia.provider.secret.dto.ProviderAuthentication;
import com.nexoia.provider.secret.exception.ProviderSecretConfigurationException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class ProviderSecretCipherTest {

    private final ProviderSecretCipher cipher = new ProviderSecretCipher(
            new ProviderSecretProperties(
                    Base64.getEncoder().encodeToString(new byte[32]),
                    1),
            new SecureRandom());

    @Test
    void encryptsAndDecryptsWithTheProviderIdentityBoundToTheCiphertext() {
        UUID providerId = UUID.randomUUID();

        var encrypted = cipher.encrypt(providerId, "server-only-api-key");

        assertThat(encrypted.encryptedValue()).doesNotContain("server-only-api-key");
        assertThat(cipher.decrypt(
                providerId,
                encrypted.encryptedValue(),
                encrypted.initializationVector(),
                encrypted.keyVersion()))
                .isEqualTo("server-only-api-key");
        assertThatThrownBy(() -> cipher.decrypt(
                UUID.randomUUID(),
                encrypted.encryptedValue(),
                encrypted.initializationVector(),
                encrypted.keyVersion()))
                .isInstanceOf(ProviderSecretConfigurationException.class);
    }

    @Test
    void neverPrintsThePlaintextCredential() {
        ProviderAuthentication authentication = ProviderAuthentication.apiKey("never-log-this");

        assertThat(authentication.toString()).isEqualTo("ProviderAuthentication[REDACTED]");
        assertThat(authentication.toString()).doesNotContain("never-log-this");
    }
}
