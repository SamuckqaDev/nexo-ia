package com.nexoia.auth.recovery.dto;

public record IssuedPasswordResetToken(String value, String hash) {
}
