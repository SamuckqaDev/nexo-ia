package com.nexoia.auth.session.security;

import com.nexoia.auth.credential.model.PasswordCredential;
import com.nexoia.auth.credential.repository.PasswordCredentialRepository;
import com.nexoia.auth.user.model.UserAccount;
import com.nexoia.auth.user.model.UserStatus;
import com.nexoia.auth.user.repository.UserAccountRepository;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NexoUserDetailsService implements UserDetailsService {

    private static final String USER_NOT_FOUND = "User not found";

    private final UserAccountRepository userAccountRepository;
    private final PasswordCredentialRepository passwordCredentialRepository;

    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String identifier) throws UsernameNotFoundException {
        UserAccount user = userAccountRepository
                .findByUsernameIgnoreCaseOrEmailIgnoreCase(identifier, identifier)
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND));
        return principal(user);
    }

    @Transactional(readOnly = true)
    public NexoUserPrincipal loadUserById(UUID userId) throws UsernameNotFoundException {
        return principal(userAccountRepository.findById(userId)
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND)));
    }

    private NexoUserPrincipal principal(UserAccount user) {
        PasswordCredential credential = passwordCredentialRepository.findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException(USER_NOT_FOUND));

        return new NexoUserPrincipal(user.getId(), user.getUsername(), user.getEmail(),
                user.getName(), user.getCreatedAt(), user.getRole(), credential.getPasswordHash(),
                user.getStatus() == UserStatus.ACTIVE);
    }
}
