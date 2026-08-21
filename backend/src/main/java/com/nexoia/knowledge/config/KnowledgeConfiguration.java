package com.nexoia.knowledge.config;

import com.nexoia.knowledge.embedding.config.KnowledgeEmbeddingProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KnowledgeEmbeddingProperties.class)
public class KnowledgeConfiguration {
}
