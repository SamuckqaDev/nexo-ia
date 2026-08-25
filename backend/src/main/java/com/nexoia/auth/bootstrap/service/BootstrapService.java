package com.nexoia.auth.bootstrap.service;

import com.nexoia.audit.dto.RecordAuditCommand;
import com.nexoia.audit.model.AuditAction;
import com.nexoia.audit.model.AuditTargetType;
import com.nexoia.audit.service.AuditService;
import com.nexoia.auth.bootstrap.dto.BootstrapStatusResponse;
import com.nexoia.auth.bootstrap.dto.CreateOwnerRequest;
import com.nexoia.auth.bootstrap.exception.BootstrapAlreadyCompletedException;
import com.nexoia.auth.credential.model.PasswordCredential;
import com.nexoia.auth.credential.repository.PasswordCredentialRepository;
import com.nexoia.auth.user.dto.UserResponse;
import com.nexoia.auth.user.exception.UserIdentityAlreadyExistsException;
import com.nexoia.auth.user.model.UserAccount;
import com.nexoia.auth.user.model.UserRole;
import com.nexoia.auth.user.model.UserStatus;
import com.nexoia.auth.user.repository.UserAccountRepository;
import com.nexoia.permission.model.ProfileKey;
import java.time.Clock;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BootstrapService {

    private final UserAccountRepository userAccountRepository;
    private final PasswordCredentialRepository passwordCredentialRepository;
    private final PasswordEncoder passwordEncoder;
    private final Clock clock;
    private final AuditService audit;

    @Transactional(readOnly = true)
    public BootstrapStatusResponse status() {
        return new BootstrapStatusResponse(userAccountRepository.count() == 0);
    }

    @Transactional
    public UserResponse createOwner(CreateOwnerRequest request) {
        if (userAccountRepository.count() != 0) {
            throw new BootstrapAlreadyCompletedException();
        }

        String username = request.username().trim().toLowerCase(Locale.ROOT);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userAccountRepository.existsByUsernameIgnoreCase(username)
                || userAccountRepository.existsByEmailIgnoreCase(email)) {
            throw new UserIdentityAlreadyExistsException();
        }

        UUID userId = UUID.randomUUID();
        Instant now = clock.instant();
        UserAccount owner = UserAccount.builder()
                .id(userId)
                .username(username)
                .email(email)
                .name(request.name().trim())
                .role(UserRole.OWNER)
                .status(UserStatus.ACTIVE)
                .assignedProfile(ProfileKey.OPERATOR)
                .build();

        try {
            owner = userAccountRepository.saveAndFlush(owner);
            passwordCredentialRepository.save(new PasswordCredential(
                    userId, passwordEncoder.encode(request.password()), now));
        } catch (DataIntegrityViolationException exception) {
            throw new BootstrapAlreadyCompletedException();
        }

        audit.record(RecordAuditCommand.success(
                AuditAction.BOOTSTRAP_OWNER_CREATED, userId, UserRole.OWNER,
                AuditTargetType.USER, userId));

        return toResponse(owner);
    }

    private UserResponse toResponse(UserAccount user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getName(),
                user.getBirthDate(), user.getCreatedAt(), user.getRole());
    }
}
