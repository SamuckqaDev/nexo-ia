package com.nexoia.auth.session.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nexoia.auth.session.dto.LoginRequest;
import com.nexoia.auth.loginattempt.service.LoginAttemptService;
import com.nexoia.auth.session.exception.InvalidCredentialsException;
import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.auth.access.dto.ClientAccessMetadata;
import com.nexoia.auth.access.service.ClientAccessService;
import com.nexoia.auth.token.dto.IssuedTokenPair;
import com.nexoia.auth.token.service.TokenCookieService;
import com.nexoia.auth.token.service.TokenSessionService;
import com.nexoia.auth.user.model.UserRole;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

@ExtendWith(MockitoExtension.class)
class AuthenticationServiceTest {

    @Mock
    private AuthenticationManager authenticationManager;
    @Mock
    private TokenSessionService tokenSessionService;
    @Mock
    private TokenCookieService tokenCookieService;
    @Mock
    private ClientAccessService clientAccessService;
    @Mock
    private LoginAttemptService loginAttemptService;

    private AuthenticationService authenticationService;

    @BeforeEach
    void setUp() {
        authenticationService = new AuthenticationService(authenticationManager,
                tokenSessionService, tokenCookieService, clientAccessService, loginAttemptService);
    }

    @Test
    void createsARevocableTokenSessionAfterValidCredentials() {
        UUID userId = UUID.randomUUID();
        Instant createdAt = Instant.parse("2026-08-16T12:00:00Z");
        var principal = new NexoUserPrincipal(userId, "owner", "owner@nexo.local", "Owner",
                createdAt, UserRole.OWNER, "encoded", true);
        Authentication authenticated = UsernamePasswordAuthenticationToken.authenticated(
                principal, null, principal.getAuthorities());
        when(authenticationManager.authenticate(any(Authentication.class))).thenReturn(authenticated);
        var request = new MockHttpServletRequest();
        var response = new MockHttpServletResponse();
        var metadata = new ClientAccessMetadata("127.0.0.1", "test");
        var tokens = new IssuedTokenPair("access", UUID.randomUUID(), createdAt.plusSeconds(300),
                "refresh", "hash", createdAt.plusSeconds(3600));
        when(clientAccessService.extract(request)).thenReturn(metadata);
        when(tokenSessionService.start(principal, metadata)).thenReturn(tokens);

        var user = authenticationService.login(
                new LoginRequest("owner", "a-strong-password"), request, response);

        assertThat(user.id()).isEqualTo(userId);
        assertThat(user.name()).isEqualTo("Owner");
        assertThat(user.createdAt()).isEqualTo(createdAt);
        assertThat(user.role()).isEqualTo(UserRole.OWNER);
        verify(tokenCookieService).write(response, tokens);
        verify(loginAttemptService).recordSuccess("owner", "127.0.0.1");
    }

    @Test
    void exposesTheSamePublicFailureForInvalidCredentials() {
        var request = new MockHttpServletRequest();
        var metadata = new ClientAccessMetadata("127.0.0.1", "test");
        when(clientAccessService.extract(request)).thenReturn(metadata);
        when(authenticationManager.authenticate(any(Authentication.class)))
                .thenThrow(new BadCredentialsException("internal detail"));

        assertThatThrownBy(() -> authenticationService.login(
                new LoginRequest("unknown", "wrong-password"),
                request, new MockHttpServletResponse()))
                .isInstanceOf(InvalidCredentialsException.class)
                .hasMessage("Invalid username, email, or password");
        verify(tokenSessionService).recordLoginFailure(metadata);
        verify(loginAttemptService).recordFailure("unknown", "127.0.0.1");
    }
}
