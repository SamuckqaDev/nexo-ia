package com.nexoia.auth.token.service;

import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.auth.token.config.TokenProperties;
import com.nexoia.auth.token.dto.IssuedTokenPair;
import com.nexoia.auth.token.exception.InvalidTokenConfigurationException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.List;
import java.util.UUID;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;
import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;
import org.springframework.security.oauth2.jwt.JwtValidators;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.NimbusJwtDecoder;
import org.springframework.security.oauth2.jwt.NimbusJwtEncoder;
import org.springframework.stereotype.Service;

@Service
public class TokenService {

    private static final int REFRESH_TOKEN_BYTES = 32;

    private final TokenProperties properties;
    private final Clock clock;
    private final SecureRandom secureRandom;
    private final JwtEncoder jwtEncoder;
    private final JwtDecoder jwtDecoder;

    public TokenService(TokenProperties properties, Clock clock) {
        this.properties = properties;
        this.clock = clock;
        secureRandom = new SecureRandom();
        SecretKey secretKey = secretKey(properties.secret());
        jwtEncoder = NimbusJwtEncoder.withSecretKey(secretKey)
                .algorithm(MacAlgorithm.HS256)
                .build();
        NimbusJwtDecoder decoder = NimbusJwtDecoder.withSecretKey(secretKey)
                .macAlgorithm(MacAlgorithm.HS256)
                .build();
        decoder.setJwtValidator(JwtValidators.createDefaultWithIssuer(properties.issuer()));
        jwtDecoder = decoder;
    }

    public IssuedTokenPair issue(NexoUserPrincipal principal, UUID sessionId) {
        Instant issuedAt = clock.instant();
        Instant accessExpiry = issuedAt.plus(properties.accessTtl());
        Instant refreshExpiry = issuedAt.plus(properties.refreshTtl());
        UUID accessJti = UUID.randomUUID();
        String refreshToken = randomRefreshToken();

        JwtClaimsSet claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(principal.userId().toString())
                .id(accessJti.toString())
                .issuedAt(issuedAt)
                .expiresAt(accessExpiry)
                .claim("sid", sessionId.toString())
                .claim("roles", List.of(principal.role().name()))
                .build();
        JwsHeader header = JwsHeader.with(MacAlgorithm.HS256).type("JWT").build();
        String accessToken = jwtEncoder.encode(JwtEncoderParameters.from(header, claims))
                .getTokenValue();

        return new IssuedTokenPair(accessToken, accessJti, accessExpiry, refreshToken,
                hashRefreshToken(refreshToken), refreshExpiry);
    }

    public Jwt decode(String token) {
        return jwtDecoder.decode(token);
    }

    public String hashRefreshToken(String token) {
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256").digest(token.getBytes(java.nio.charset.StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new InvalidTokenConfigurationException("SHA-256 is unavailable", exception);
        }
    }

    private String randomRefreshToken() {
        byte[] value = new byte[REFRESH_TOKEN_BYTES];
        secureRandom.nextBytes(value);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(value);
    }

    private SecretKey secretKey(String encodedSecret) {
        try {
            byte[] decoded = Base64.getDecoder().decode(encodedSecret);
            if (decoded.length < 32) {
                throw new InvalidTokenConfigurationException(
                        "NEXO_JWT_SECRET must contain at least 32 random bytes encoded as Base64");
            }
            return new SecretKeySpec(decoded, "HmacSHA256");
        } catch (IllegalArgumentException exception) {
            throw new InvalidTokenConfigurationException("NEXO_JWT_SECRET must be valid Base64", exception);
        }
    }
}
