package com.nexoia.auth.profile.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

import com.nexoia.auth.profile.dto.UpdateProfileRequest;
import com.nexoia.auth.user.exception.UserIdentityAlreadyExistsException;
import com.nexoia.auth.user.model.UserAccount;
import com.nexoia.auth.user.model.UserRole;
import com.nexoia.auth.user.model.UserStatus;
import com.nexoia.auth.user.repository.UserAccountRepository;
import java.time.Instant;
import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock
    private UserAccountRepository repository;
    private ProfileService service;

    @BeforeEach
    void setUp() {
        service = new ProfileService(repository);
    }

    @Test
    void updatesTheAuthenticatedUsersProfile() {
        UUID userId = UUID.randomUUID();
        UserAccount user = user(userId);
        when(repository.findById(userId)).thenReturn(Optional.of(user));
        when(repository.saveAndFlush(user)).thenReturn(user);

        var response = service.update(userId, new UpdateProfileRequest(
                " New.Username ", " NEW@NEXO.LOCAL ", "New Name", LocalDate.of(1997, 8, 17)));

        assertThat(response.username()).isEqualTo("new.username");
        assertThat(response.email()).isEqualTo("new@nexo.local");
        assertThat(response.birthDate()).isEqualTo(LocalDate.of(1997, 8, 17));
    }

    @Test
    void rejectsAUsernameOwnedByAnotherUser() {
        UUID userId = UUID.randomUUID();
        when(repository.findById(userId)).thenReturn(Optional.of(user(userId)));
        when(repository.existsByUsernameIgnoreCaseAndIdNot("taken", userId)).thenReturn(true);

        assertThatThrownBy(() -> service.update(userId, new UpdateProfileRequest(
                "taken", "owner@nexo.local", "Owner", null)))
                .isInstanceOf(UserIdentityAlreadyExistsException.class);
    }

    private UserAccount user(UUID userId) {
        Instant now = Instant.parse("2026-08-17T12:00:00Z");
        return UserAccount.builder().id(userId).username("owner").email("owner@nexo.local")
                .name("Owner").role(UserRole.OWNER).status(UserStatus.ACTIVE)
                .createdAt(now).updatedAt(now).build();
    }
}
