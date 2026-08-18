package com.nexoia.conversation.inference.config;

import java.time.Duration;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Transport limits for a streamed model request. The timeout bounds how long a local generation may
 * hold an open response before Nexo IA closes it and records the failure.
 */
@ConfigurationProperties(prefix = "nexo.conversation.stream")
public record ModelStreamProperties(Duration timeout) {}
