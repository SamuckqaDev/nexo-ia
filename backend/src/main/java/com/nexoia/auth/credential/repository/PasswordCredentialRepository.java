package com.nexoia.auth.credential.repository;

import com.nexoia.auth.credential.model.PasswordCredential;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface PasswordCredentialRepository extends JpaRepository<PasswordCredential, UUID> {
}
