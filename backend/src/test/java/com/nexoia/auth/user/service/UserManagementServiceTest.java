package com.nexoia.auth.user.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.nexoia.audit.service.AuditService;
import com.nexoia.auth.access.dto.ClientAccessMetadata;
import com.nexoia.auth.access.repository.AccessEventRepository;
import com.nexoia.auth.access.service.ClientAccessService;
import com.nexoia.auth.credential.repository.PasswordCredentialRepository;
import com.nexoia.auth.session.model.AuthSession;
import com.nexoia.auth.session.model.SessionStatus;
import com.nexoia.auth.session.repository.AuthSessionRepository;
import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.auth.token.service.TokenCookieService;
import com.nexoia.auth.token.service.TokenSessionService;
import com.nexoia.auth.user.dto.UpdateUserStatusRequest;
import com.nexoia.auth.user.dto.CreateMemberRequest;
import com.nexoia.auth.user.exception.OwnerStatusChangeException;
import com.nexoia.auth.user.model.UserAccount;
import com.nexoia.auth.user.model.UserRole;
import com.nexoia.auth.user.model.UserStatus;
import com.nexoia.auth.user.repository.UserAccountRepository;
import com.nexoia.permission.model.ProfileKey;
import com.nexoia.permission.service.PermissionAdminService;
import jakarta.servlet.http.HttpServletRequest;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserManagementServiceTest {
    private static final Instant NOW = Instant.parse("2026-08-17T02:00:00Z");
    private static final UUID OWNER_ID = UUID.randomUUID();
    @Mock UserAccountRepository users;
    @Mock PasswordCredentialRepository credentials;
    @Mock AuthSessionRepository sessions;
    @Mock AccessEventRepository events;
    @Mock PasswordEncoder encoder;
    @Mock TokenCookieService cookies;
    @Mock TokenSessionService tokens;
    @Mock ClientAccessService access;
    @Mock HttpServletRequest request;
    private UserManagementService service;

    @BeforeEach void setUp() {
        service = new UserManagementService(users, credentials, sessions, events, encoder,
                cookies, tokens, access, new PermissionAdminService(),
                Clock.fixed(NOW, ZoneOffset.UTC),
                mock(AuditService.class));
    }

    @Test void createsAMemberWithTheDefaultProfileWhenNoneIsRequested() {
        when(users.existsByUsernameIgnoreCase("newuser")).thenReturn(false);
        when(users.existsByEmailIgnoreCase("new@nexo.local")).thenReturn(false);
        when(users.saveAndFlush(any(UserAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(encoder.encode(any())).thenReturn("hash");
        when(cookies.accessToken(request)).thenReturn(Optional.of("token"));
        when(tokens.sessionId("token")).thenReturn(UUID.randomUUID());
        when(access.extract(request)).thenReturn(new ClientAccessMetadata("127.0.0.1", "test"));

        var response = service.createMember(principal(), new CreateMemberRequest(
                "newuser", "new@nexo.local", "New User", "Str0ng-Passw0rd!", null), request);

        assertThat(response.role()).isEqualTo(UserRole.MEMBER);
        assertThat(response.assignedProfile()).isEqualTo(ProfileKey.RESEARCHER);
    }

    @Test void createsAMemberWithTheRequestedProfile() {
        when(users.existsByUsernameIgnoreCase("reader1")).thenReturn(false);
        when(users.existsByEmailIgnoreCase("reader@nexo.local")).thenReturn(false);
        when(users.saveAndFlush(any(UserAccount.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(encoder.encode(any())).thenReturn("hash");
        when(cookies.accessToken(request)).thenReturn(Optional.of("token"));
        when(tokens.sessionId("token")).thenReturn(UUID.randomUUID());
        when(access.extract(request)).thenReturn(new ClientAccessMetadata("127.0.0.1", "test"));

        var response = service.createMember(principal(), new CreateMemberRequest(
                "reader1", "reader@nexo.local", "Reader One", "Str0ng-Passw0rd!",
                ProfileKey.READER), request);

        assertThat(response.assignedProfile()).isEqualTo(ProfileKey.READER);
    }

    @Test void disablingAMemberRevokesEveryActiveSession() {
        UUID memberId = UUID.randomUUID(); UUID actorSession = UUID.randomUUID();
        UserAccount member = user(memberId, UserRole.MEMBER);
        AuthSession session = AuthSession.builder().id(UUID.randomUUID()).userId(memberId)
                .status(SessionStatus.ACTIVE).refreshExpiresAt(NOW.plusSeconds(60)).build();
        when(users.findById(memberId)).thenReturn(Optional.of(member));
        when(sessions.findAllByUserIdAndStatusOrderByLastSeenAtDesc(memberId, SessionStatus.ACTIVE))
                .thenReturn(List.of(session));
        when(cookies.accessToken(request)).thenReturn(Optional.of("token"));
        when(tokens.sessionId("token")).thenReturn(actorSession);
        when(access.extract(request)).thenReturn(new ClientAccessMetadata("127.0.0.1", "test"));

        service.changeStatus(principal(), memberId,
                new UpdateUserStatusRequest(UserStatus.DISABLED), request);

        assertThat(member.getStatus()).isEqualTo(UserStatus.DISABLED);
        assertThat(session.getStatus()).isEqualTo(SessionStatus.REVOKED);
        assertThat(session.getRevokeReason()).isEqualTo("USER_DISABLED_BY_OWNER");
    }

    @Test void neverAllowsTheOwnerToBeDisabled() {
        when(users.findById(OWNER_ID)).thenReturn(Optional.of(user(OWNER_ID, UserRole.OWNER)));
        assertThatThrownBy(() -> service.changeStatus(principal(), OWNER_ID,
                new UpdateUserStatusRequest(UserStatus.DISABLED), request))
                .isInstanceOf(OwnerStatusChangeException.class);
    }

    private UserAccount user(UUID id, UserRole role) {
        return UserAccount.builder().id(id).username("user").email("user@nexo.local")
                .name("User").role(role).status(UserStatus.ACTIVE)
                .createdAt(NOW).updatedAt(NOW).build();
    }

    private NexoUserPrincipal principal() {
        return new NexoUserPrincipal(OWNER_ID, "owner", "owner@nexo.local", "Owner", NOW,
                UserRole.OWNER, "hash", true);
    }
}
