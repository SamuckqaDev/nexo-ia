package com.nexoia.knowledge.config;

import com.nexoia.knowledge.embedding.config.KnowledgeEmbeddingProperties;
import com.nexoia.knowledge.retrieval.config.KnowledgeRetrievalProperties;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({KnowledgeEmbeddingProperties.class, KnowledgeRetrievalProperties.class})
public class KnowledgeConfiguration {
}
