package com.nexoia.auth.user.dto;

import com.nexoia.auth.credential.validation.ValidPassword;
import com.nexoia.permission.model.ProfileKey;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record CreateMemberRequest(
        @NotBlank @Size(min = 3, max = 64)
        @Pattern(regexp = "^[a-zA-Z0-9._-]+$",
                message = "must contain only letters, numbers, dot, underscore or hyphen")
        String username,
        @NotBlank @Email @Size(max = 254) String email,
        @NotBlank @Size(min = 2, max = 120) String name,
        @NotBlank @ValidPassword String password,
        ProfileKey profile) {
}
