package com.nexoia.auth.session.repository;

import com.nexoia.auth.session.model.AuthSession;
import com.nexoia.auth.session.model.SessionStatus;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AuthSessionRepository extends JpaRepository<AuthSession, UUID> {

    List<AuthSession> findAllByUserIdAndStatusOrderByLastSeenAtDesc(UUID userId, SessionStatus status);

    Optional<AuthSession> findByIdAndUserIdAndStatus(UUID id, UUID userId, SessionStatus status);
}
