package com.nexoia.auth.credential.dto;

import com.nexoia.auth.credential.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record ChangePasswordRequest(
        @NotBlank @Size(max = 128) String currentPassword,
        @NotBlank @ValidPassword String newPassword) {
}
