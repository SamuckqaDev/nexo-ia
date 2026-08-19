package com.nexoia.provider.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.provider.dto.CreateProviderRequest;
import com.nexoia.provider.exception.InvalidProviderEndpointException;
import com.nexoia.provider.exception.ProviderConfigurationConflictException;
import com.nexoia.provider.exception.ProviderConfigurationNotFoundException;
import com.nexoia.provider.model.ProviderConfiguration;
import com.nexoia.provider.model.ProviderType;
import com.nexoia.provider.repository.ProviderConfigurationRepository;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProviderRegistryServiceTest {

    @Mock
    private ProviderConfigurationRepository repository;
    @Mock
    private com.nexoia.audit.service.AuditService audit;
    private ProviderRegistryService service;

    private final UUID userId = UUID.randomUUID();
    private final UUID providerId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new ProviderRegistryService(repository, audit);
    }

    @Test
    void savesANormalizedEndpointForTheAuthenticatedUser() {
        when(repository.existsByUserIdAndEndpoint(userId, "http://127.0.0.1:11434")).thenReturn(false);
        when(repository.save(any(ProviderConfiguration.class))).thenAnswer(call -> call.getArgument(0));

        var response = service.create(userId, request("  http://127.0.0.1:11434  "));

        assertThat(response.endpoint()).isEqualTo("http://127.0.0.1:11434");
        assertThat(response.enabled()).isTrue();
    }

    @ParameterizedTest
    @ValueSource(strings = {
            "file:///etc/passwd",
            "//127.0.0.1:11434",
            "ftp://127.0.0.1",
            "http://user:secret@127.0.0.1:11434",
            "http://127.0.0.1:11434?probe=1",
            "http://127.0.0.1:11434#fragment",
            "not-a-url"
    })
    void rejectsAnEndpointThatIsNotAPlainHttpOrHttpsUrl(String endpoint) {
        assertThatThrownBy(() -> service.create(userId, request(endpoint)))
                .isInstanceOf(InvalidProviderEndpointException.class);
        verify(repository, never()).save(any(ProviderConfiguration.class));
    }

    @Test
    void rejectsAnEndpointAlreadyConfiguredByTheSameUser() {
        when(repository.existsByUserIdAndEndpoint(userId, "http://127.0.0.1:11434")).thenReturn(true);

        assertThatThrownBy(() -> service.create(userId, request("http://127.0.0.1:11434")))
                .isInstanceOf(ProviderConfigurationConflictException.class);
    }

    @Test
    void hidesAnotherUsersProviderFromUpdates() {
        when(repository.findByIdAndUserId(providerId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.update(userId, providerId, request("http://127.0.0.1:11434")))
                .isInstanceOf(ProviderConfigurationNotFoundException.class);
    }

    @Test
    void hidesAnotherUsersProviderFromRemoval() {
        when(repository.findByIdAndUserId(providerId, userId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.remove(userId, providerId))
                .isInstanceOf(ProviderConfigurationNotFoundException.class);
        verify(repository, never()).delete(any(ProviderConfiguration.class));
    }

    private CreateProviderRequest request(String endpoint) {
        return new CreateProviderRequest(ProviderType.OLLAMA, "Local Ollama", endpoint, null);
    }
}
