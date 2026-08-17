package com.nexoia.auth.loginattempt.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("nexo.security.login-throttle")
public record LoginThrottleProperties(
        Duration observationWindow,
        int firstThreshold,
        Duration firstLock,
        int secondThreshold,
        Duration secondLock,
        int maximumThreshold,
        Duration maximumLock) {
}
