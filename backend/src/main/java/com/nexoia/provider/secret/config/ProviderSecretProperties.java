package com.nexoia.provider.secret.config;

import java.util.Base64;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.context.properties.bind.DefaultValue;

/** Server-owned key material used only to encrypt provider credentials at rest. */
@ConfigurationProperties(prefix = "nexo.security.secrets")
public record ProviderSecretProperties(
        @DefaultValue("") String masterKey,
        @DefaultValue("1") int keyVersion) {

    public byte[] decodedKey() {
        if (masterKey == null || masterKey.isBlank()) {
            return new byte[0];
        }
        try {
            return Base64.getDecoder().decode(masterKey.trim());
        } catch (IllegalArgumentException exception) {
            return new byte[0];
        }
    }
}
