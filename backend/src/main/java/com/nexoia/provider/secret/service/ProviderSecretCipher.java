package com.nexoia.provider.secret.service;

import com.nexoia.provider.secret.config.ProviderSecretProperties;
import com.nexoia.provider.secret.exception.ProviderSecretConfigurationException;
import java.nio.charset.StandardCharsets;
import java.security.GeneralSecurityException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.UUID;
import javax.crypto.Cipher;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/** AES-GCM encryption with provider identity bound as authenticated additional data. */
@Component
public class ProviderSecretCipher {

    private static final String TRANSFORMATION = "AES/GCM/NoPadding";
    private static final int IV_BYTES = 12;
    private static final int TAG_BITS = 128;
    private static final int KEY_BYTES = 32;

    private final ProviderSecretProperties properties;
    private final SecureRandom secureRandom;

    @Autowired
    public ProviderSecretCipher(ProviderSecretProperties properties) {
        this(properties, new SecureRandom());
    }

    ProviderSecretCipher(ProviderSecretProperties properties, SecureRandom secureRandom) {
        this.properties = properties;
        this.secureRandom = secureRandom;
    }

    public EncryptedProviderSecret encrypt(UUID providerId, String plaintext) {
        byte[] iv = new byte[IV_BYTES];
        secureRandom.nextBytes(iv);
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            cipher.init(Cipher.ENCRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(providerId.toString().getBytes(StandardCharsets.UTF_8));
            byte[] ciphertext = cipher.doFinal(plaintext.getBytes(StandardCharsets.UTF_8));
            return new EncryptedProviderSecret(
                    Base64.getEncoder().encodeToString(ciphertext),
                    Base64.getEncoder().encodeToString(iv),
                    properties.keyVersion());
        } catch (GeneralSecurityException exception) {
            throw new ProviderSecretConfigurationException(exception);
        }
    }

    public String decrypt(
            UUID providerId,
            String encryptedValue,
            String initializationVector,
            int keyVersion) {
        if (keyVersion != properties.keyVersion()) {
            throw new ProviderSecretConfigurationException();
        }
        try {
            Cipher cipher = Cipher.getInstance(TRANSFORMATION);
            byte[] iv = Base64.getDecoder().decode(initializationVector);
            cipher.init(Cipher.DECRYPT_MODE, key(), new GCMParameterSpec(TAG_BITS, iv));
            cipher.updateAAD(providerId.toString().getBytes(StandardCharsets.UTF_8));
            byte[] plaintext = cipher.doFinal(Base64.getDecoder().decode(encryptedValue));
            return new String(plaintext, StandardCharsets.UTF_8);
        } catch (GeneralSecurityException | IllegalArgumentException exception) {
            throw new ProviderSecretConfigurationException(exception);
        }
    }

    private SecretKeySpec key() {
        byte[] decoded = properties.decodedKey();
        if (decoded.length != KEY_BYTES) {
            throw new ProviderSecretConfigurationException();
        }
        return new SecretKeySpec(decoded, "AES");
    }

    public record EncryptedProviderSecret(
            String encryptedValue,
            String initializationVector,
            int keyVersion) {
    }
}
