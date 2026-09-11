package com.nexoia.provider.secret.service;

import com.nexoia.provider.model.ProviderConfiguration;
import com.nexoia.provider.model.ProviderType;
import com.nexoia.provider.secret.dto.ProviderAuthentication;
import com.nexoia.provider.secret.exception.ProviderCredentialRequiredException;
import com.nexoia.provider.secret.model.ProviderSecret;
import com.nexoia.provider.secret.repository.ProviderSecretRepository;
import com.nexoia.provider.secret.service.ProviderSecretCipher.EncryptedProviderSecret;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Owns encrypted provider credentials; plaintext is available only for one server-side request. */
@Service
public class ProviderSecretService {

    private final ProviderSecretRepository repository;
    private final ProviderSecretCipher cipher;

    @Autowired
    public ProviderSecretService(
            ProviderSecretRepository repository,
            ProviderSecretCipher cipher) {
        this.repository = repository;
        this.cipher = cipher;
    }

    @Transactional
    public void replace(UUID providerId, String apiKey) {
        if (apiKey == null || apiKey.isBlank()) {
            return;
        }
        EncryptedProviderSecret encrypted = cipher.encrypt(providerId, apiKey.trim());
        ProviderSecret secret = repository.findById(providerId)
                .orElseGet(() -> ProviderSecret.builder()
                        .providerConfigurationId(providerId)
                        .encryptedValue(encrypted.encryptedValue())
                        .initializationVector(encrypted.initializationVector())
                        .keyVersion(encrypted.keyVersion())
                        .build());
        secret.replace(
                encrypted.encryptedValue(),
                encrypted.initializationVector(),
                encrypted.keyVersion());
        repository.save(secret);
    }

    @Transactional(readOnly = true)
    public ProviderAuthentication resolve(ProviderConfiguration provider) {
        ProviderSecret secret = repository.findById(provider.getId()).orElse(null);
        if (secret == null) {
            if (provider.getProviderType().requiresCredential()) {
                throw new ProviderCredentialRequiredException();
            }
            return ProviderAuthentication.none();
        }
        return ProviderAuthentication.apiKey(cipher.decrypt(
                provider.getId(),
                secret.getEncryptedValue(),
                secret.getInitializationVector(),
                secret.getKeyVersion()));
    }

    @Transactional(readOnly = true)
    public boolean configured(UUID providerId) {
        return repository.existsById(providerId);
    }

    @Transactional
    public void remove(UUID providerId) {
        repository.deleteById(providerId);
    }

    public void requireFor(ProviderType providerType, String apiKey, boolean existingConfigured) {
        if (providerType.requiresCredential()
                && (apiKey == null || apiKey.isBlank())
                && !existingConfigured) {
            throw new ProviderCredentialRequiredException();
        }
    }
}
