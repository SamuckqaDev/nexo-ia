package com.nexoia.conversation.inference.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({ConversationContextProperties.class, ModelStreamProperties.class})
public class InferenceConfiguration {

    /**
     * Model streaming blocks on network I/O for as long as a generation lasts. Virtual threads make
     * one carrier-free thread per request affordable, which is why Nexo IA targets Java 25.
     */
    @Bean(destroyMethod = "shutdown")
    ExecutorService modelRequestExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
