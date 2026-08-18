package com.nexoia.auth.profile.service;

import com.nexoia.auth.profile.dto.UpdateProfileRequest;
import com.nexoia.auth.user.dto.UserResponse;
import com.nexoia.auth.user.exception.UserIdentityAlreadyExistsException;
import com.nexoia.auth.user.exception.UserNotFoundException;
import com.nexoia.auth.user.model.UserAccount;
import com.nexoia.auth.user.repository.UserAccountRepository;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ProfileService {

    private final UserAccountRepository userAccountRepository;

    @Transactional
    public UserResponse update(UUID userId, UpdateProfileRequest request) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        String username = request.username().trim().toLowerCase(Locale.ROOT);
        String email = request.email().trim().toLowerCase(Locale.ROOT);
        if (userAccountRepository.existsByUsernameIgnoreCaseAndIdNot(username, userId)
                || userAccountRepository.existsByEmailIgnoreCaseAndIdNot(email, userId)) {
            throw new UserIdentityAlreadyExistsException();
        }

        user.updateProfile(username, email, request.name().trim(), request.birthDate());
        try {
            return toResponse(userAccountRepository.saveAndFlush(user));
        } catch (DataIntegrityViolationException exception) {
            throw new UserIdentityAlreadyExistsException();
        }
    }

    private UserResponse toResponse(UserAccount user) {
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getName(),
                user.getBirthDate(), user.getCreatedAt(), user.getRole());
    }
}
