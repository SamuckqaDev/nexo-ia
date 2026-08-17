package com.nexoia.auth.recovery.service;

import com.nexoia.auth.recovery.dto.IssuedPasswordResetToken;
import com.nexoia.auth.token.exception.InvalidTokenConfigurationException;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.util.Base64;
import java.util.HexFormat;
import org.springframework.stereotype.Service;

@Service
public class PasswordResetTokenService {

    private static final int TOKEN_BYTES = 32;
    private final SecureRandom secureRandom = new SecureRandom();

    public IssuedPasswordResetToken issue() {
        byte[] randomValue = new byte[TOKEN_BYTES];
        secureRandom.nextBytes(randomValue);
        String value = Base64.getUrlEncoder().withoutPadding().encodeToString(randomValue);
        return new IssuedPasswordResetToken(value, hash(value));
    }

    public String hash(String value) {
        try {
            byte[] digest = MessageDigest.getInstance("SHA-256")
                    .digest(value.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(digest);
        } catch (NoSuchAlgorithmException exception) {
            throw new InvalidTokenConfigurationException("SHA-256 is unavailable", exception);
        }
    }
}
