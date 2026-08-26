package com.nexoia.media.image.config;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(ComfyUiProperties.class)
public class ImageGenerationConfiguration {

    @Bean(destroyMethod = "shutdown")
    ExecutorService imageGenerationExecutor() {
        return Executors.newVirtualThreadPerTaskExecutor();
    }
}
