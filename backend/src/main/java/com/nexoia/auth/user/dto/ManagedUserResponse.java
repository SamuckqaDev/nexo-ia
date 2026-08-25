package com.nexoia.auth.user.dto;

import com.nexoia.auth.user.model.UserRole;
import com.nexoia.auth.user.model.UserStatus;
import com.nexoia.permission.model.ProfileKey;
import java.time.Instant;
import java.util.UUID;

public record ManagedUserResponse(
        UUID id,
        String username,
        String email,
        String name,
        UserRole role,
        UserStatus status,
        ProfileKey assignedProfile,
        Instant createdAt,
        Instant updatedAt) {
}
