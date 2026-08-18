package com.nexoia.provider.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexoia.provider.exception.InvalidProviderEndpointException;
import com.nexoia.provider.exception.ProviderEndpointNotAllowedException;
import com.nexoia.provider.model.ProcessingLocation;
import com.nexoia.provider.model.ProviderType;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

class ProviderEndpointGuardTest {

    private final ProviderEndpointGuard guard = new ProviderEndpointGuard();

    @Test
    void reportsLoopbackOllamaAsLocalProcessing() {
        assertThat(guard.verify(ProviderType.OLLAMA, "http://127.0.0.1:11434"))
                .isEqualTo(ProcessingLocation.LOCAL);
    }

    @Test
    void reportsAHomeServerOllamaAsRemoteProcessing() {
        assertThat(guard.verify(ProviderType.OLLAMA, "http://192.168.1.40:11434"))
                .isEqualTo(ProcessingLocation.REMOTE);
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "http://127.0.0.1:11434",
            "http://192.168.1.40:11434",
            "http://169.254.169.254/latest/meta-data",
            "http://0.0.0.0:11434"
    })
    void blocksAVendorProviderPointedAtAPrivateAddress(String endpoint) {
        assertThatThrownBy(() -> guard.verify(ProviderType.OPENAI, endpoint))
                .isInstanceOf(ProviderEndpointNotAllowedException.class)
                .hasMessage("This provider type cannot use the configured endpoint");
    }

    @Test
    void allowsASelfHostedOpenAiCompatibleServerOnAPrivateNetwork() {
        assertThat(guard.verify(ProviderType.OPENAI_COMPATIBLE, "http://10.0.0.8:8000"))
                .isEqualTo(ProcessingLocation.REMOTE);
    }

    @Test
    void rejectsAnEndpointWithoutAResolvableHost() {
        assertThatThrownBy(() -> guard.verify(ProviderType.OLLAMA, "http://nexo.invalid"))
                .isInstanceOf(InvalidProviderEndpointException.class);
    }
}
