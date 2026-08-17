package com.nexoia.auth.bootstrap.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.auth.bootstrap.dto.CreateOwnerRequest;
import com.nexoia.auth.bootstrap.exception.BootstrapAlreadyCompletedException;
import com.nexoia.auth.credential.model.PasswordCredential;
import com.nexoia.auth.credential.repository.PasswordCredentialRepository;
import com.nexoia.auth.user.model.UserAccount;
import com.nexoia.auth.user.model.UserRole;
import com.nexoia.auth.user.repository.UserAccountRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class BootstrapServiceTest {

    private static final Instant NOW = Instant.parse("2026-08-16T12:00:00Z");

    @Mock
    private UserAccountRepository userAccountRepository;
    @Mock
    private PasswordCredentialRepository passwordCredentialRepository;
    @Mock
    private PasswordEncoder passwordEncoder;
    private BootstrapService bootstrapService;

    @BeforeEach
    void setUp() {
        bootstrapService = new BootstrapService(userAccountRepository, passwordCredentialRepository,
                passwordEncoder, Clock.fixed(NOW, ZoneOffset.UTC));
    }

    @Test
    void createsTheFirstOwnerWithNormalizedIdentityAndHashedPassword() {
        when(passwordEncoder.encode("a-strong-password")).thenReturn("encoded-password");
        when(userAccountRepository.saveAndFlush(any(UserAccount.class)))
                .thenAnswer(invocation -> {
                    UserAccount submitted = invocation.getArgument(0);
                    return UserAccount.builder()
                            .id(submitted.getId())
                            .username(submitted.getUsername())
                            .email(submitted.getEmail())
                            .name(submitted.getName())
                            .role(submitted.getRole())
                            .status(submitted.getStatus())
                            .createdAt(NOW)
                            .updatedAt(NOW)
                            .build();
                });

        var response = bootstrapService.createOwner(new CreateOwnerRequest(
                " Nexo.Owner ", " OWNER@NEXO.LOCAL ", " Nexo Owner ", "a-strong-password"));

        assertThat(response.username()).isEqualTo("nexo.owner");
        assertThat(response.email()).isEqualTo("owner@nexo.local");
        assertThat(response.name()).isEqualTo("Nexo Owner");
        assertThat(response.role()).isEqualTo(UserRole.OWNER);
        assertThat(response.createdAt()).isEqualTo(NOW);

        ArgumentCaptor<PasswordCredential> credential = ArgumentCaptor.forClass(PasswordCredential.class);
        verify(passwordCredentialRepository).save(credential.capture());
        assertThat(credential.getValue().getPasswordHash()).isEqualTo("encoded-password");
        assertThat(credential.getValue().getChangedAt()).isEqualTo(NOW);
    }

    @Test
    void rejectsBootstrapAfterAnyUserExists() {
        when(userAccountRepository.count()).thenReturn(1L);

        assertThatThrownBy(() -> bootstrapService.createOwner(new CreateOwnerRequest(
                "owner", "owner@nexo.local", "Owner", "a-strong-password")))
                .isInstanceOf(BootstrapAlreadyCompletedException.class);

        verify(passwordEncoder, never()).encode(any());
        verify(userAccountRepository, never()).saveAndFlush(any());
    }
}
