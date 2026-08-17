package com.nexoia.auth.token.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("nexo.security.token")
public record TokenProperties(
        String issuer,
        String secret,
        Duration accessTtl,
        Duration refreshTtl,
        boolean secureCookie) {
}
