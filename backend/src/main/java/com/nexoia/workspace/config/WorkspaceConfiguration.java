package com.nexoia.workspace.config;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(WorkspaceProperties.class)
public class WorkspaceConfiguration {
}
