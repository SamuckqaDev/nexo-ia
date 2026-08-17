package com.nexoia.auth.recovery.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("nexo.security.password-recovery")
public record PasswordRecoveryProperties(Duration tokenTtl, String frontendResetUrl, String sender) {
}
