package com.nexoia.auth.loginattempt.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(LoginThrottleProperties.class)
public class LoginAttemptConfiguration {
}
