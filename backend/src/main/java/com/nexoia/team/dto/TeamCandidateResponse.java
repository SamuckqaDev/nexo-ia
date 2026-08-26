package com.nexoia.team.dto;

import com.nexoia.auth.user.model.UserRole;
import com.nexoia.permission.model.ProfileKey;
import java.util.UUID;

/** An active Nexo user an administrator may add to a Team. */
public record TeamCandidateResponse(
        UUID userId,
        String username,
        String name,
        String email,
        UserRole role,
        ProfileKey assignedProfile) {
}
