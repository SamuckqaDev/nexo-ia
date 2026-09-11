package com.nexoia.provider.secret.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ProviderSecretProperties.class)
public class ProviderSecretConfiguration {
}
