package com.nexoia.auth.session.service;

import com.nexoia.auth.session.dto.LoginRequest;
import com.nexoia.auth.loginattempt.service.LoginAttemptService;
import com.nexoia.auth.session.exception.InvalidCredentialsException;
import com.nexoia.auth.session.exception.UnauthenticatedSessionException;
import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.auth.access.dto.ClientAccessMetadata;
import com.nexoia.auth.access.service.ClientAccessService;
import com.nexoia.auth.token.exception.InvalidRefreshTokenException;
import com.nexoia.auth.token.service.TokenCookieService;
import com.nexoia.auth.token.service.TokenSessionService;
import com.nexoia.auth.user.dto.UserResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final TokenSessionService tokenSessionService;
    private final TokenCookieService tokenCookieService;
    private final ClientAccessService clientAccessService;
    private final LoginAttemptService loginAttemptService;

    public UserResponse login(LoginRequest request, HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        ClientAccessMetadata metadata = clientAccessService.extract(httpRequest);
        loginAttemptService.assertAllowed(request.identifier(), metadata.ipAddress());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.identifier().trim(), request.password()));

            NexoUserPrincipal principal = (NexoUserPrincipal) authentication.getPrincipal();
            tokenCookieService.write(httpResponse, tokenSessionService.start(principal, metadata));
            loginAttemptService.recordSuccess(request.identifier(), metadata.ipAddress());
            return toResponse(principal);
        } catch (BadCredentialsException | DisabledException exception) {
            loginAttemptService.recordFailure(request.identifier(), metadata.ipAddress());
            tokenSessionService.recordLoginFailure(metadata);
            throw new InvalidCredentialsException();
        }
    }

    public void refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = tokenCookieService.refreshToken(request)
                .orElseThrow(InvalidRefreshTokenException::new);
        tokenCookieService.write(response, tokenSessionService.refresh(
                refreshToken, clientAccessService.extract(request)));
    }

    public UserResponse currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof NexoUserPrincipal principal)) {
            throw new UnauthenticatedSessionException();
        }
        return toResponse(principal);
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        tokenCookieService.accessToken(request).ifPresent(accessToken ->
                tokenSessionService.revokeAccessToken(
                        accessToken, clientAccessService.extract(request)));
        tokenCookieService.clear(response);
    }

    private UserResponse toResponse(NexoUserPrincipal principal) {
        return new UserResponse(principal.userId(), principal.username(), principal.email(),
                principal.name(), principal.createdAt(), principal.role());
    }
}
