package com.nexoia.provider.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexoia.provider.exception.InvalidProviderEndpointException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProviderEndpointNormalizerTest {

    private final ProviderEndpointNormalizer withGateway =
            new ProviderEndpointNormalizer("host.containers.internal");
    private final ProviderEndpointNormalizer withoutGateway = new ProviderEndpointNormalizer("");

    @ParameterizedTest
    @ValueSource(strings = {
            "http://localhost:11434",
            "http://127.0.0.1:11434",
            "http://0.0.0.0:11434"
    })
    void rewritesALoopbackHostToTheConfiguredGateway(String endpoint) {
        assertThat(withGateway.normalize(endpoint)).isEqualTo("http://host.containers.internal:11434");
    }

    @Test
    void preservesThePortAndPathWhileRewriting() {
        assertThat(withGateway.normalize("http://localhost:11434/ollama"))
                .isEqualTo("http://host.containers.internal:11434/ollama");
    }

    @Test
    void leavesANonLoopbackHostUnchanged() {
        assertThat(withGateway.normalize("http://192.168.1.40:11434"))
                .isEqualTo("http://192.168.1.40:11434");
    }

    @Test
    void doesNotRewriteWhenNoGatewayIsConfigured() {
        assertThat(withoutGateway.normalize("http://localhost:11434"))
                .isEqualTo("http://localhost:11434");
    }

    @Test
    void stillRejectsAMalformedEndpoint() {
        assertThatThrownBy(() -> withGateway.normalize("not-a-url"))
                .isInstanceOf(InvalidProviderEndpointException.class);
    }
}
