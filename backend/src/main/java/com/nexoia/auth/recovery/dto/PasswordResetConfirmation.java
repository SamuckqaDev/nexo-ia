package com.nexoia.auth.recovery.dto;

import com.nexoia.auth.credential.validation.ValidPassword;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetConfirmation(
        @NotBlank @Size(max = 128) String token,
        @NotBlank @ValidPassword String password) {
}
