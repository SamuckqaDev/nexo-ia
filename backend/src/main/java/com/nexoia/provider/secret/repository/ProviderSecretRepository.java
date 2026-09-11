package com.nexoia.provider.secret.repository;

import com.nexoia.provider.secret.model.ProviderSecret;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ProviderSecretRepository extends JpaRepository<ProviderSecret, UUID> {
}
