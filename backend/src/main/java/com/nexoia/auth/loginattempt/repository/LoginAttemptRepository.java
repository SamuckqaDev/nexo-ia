package com.nexoia.auth.loginattempt.repository;

import com.nexoia.auth.loginattempt.model.LoginAttempt;
import java.time.Instant;
import java.util.List;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LoginAttemptRepository extends JpaRepository<LoginAttempt, Long> {

    List<LoginAttempt> findTop20ByIdentifierHashAndIpAddressAndOccurredAtAfterOrderByOccurredAtDesc(
            String identifierHash, String ipAddress, Instant occurredAfter);
}
