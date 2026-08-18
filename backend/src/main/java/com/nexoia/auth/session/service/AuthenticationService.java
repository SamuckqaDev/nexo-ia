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
import com.nexoia.auth.user.exception.UserNotFoundException;
import com.nexoia.auth.user.model.UserAccount;
import com.nexoia.auth.user.repository.UserAccountRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.DisabledException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthenticationService {

    private final AuthenticationManager authenticationManager;
    private final TokenSessionService tokenSessionService;
    private final TokenCookieService tokenCookieService;
    private final ClientAccessService clientAccessService;
    private final LoginAttemptService loginAttemptService;
    private final UserAccountRepository userAccountRepository;

    public UserResponse login(LoginRequest request, HttpServletRequest httpRequest,
            HttpServletResponse httpResponse) {
        ClientAccessMetadata metadata = clientAccessService.extract(httpRequest);
        log.info("[NEXO-BACK][AUTH] Login attempt identifierType={} ip={}", identifierType(request.identifier()), metadata.ipAddress());
        loginAttemptService.assertAllowed(request.identifier(), metadata.ipAddress());
        try {
            Authentication authentication = authenticationManager.authenticate(
                    UsernamePasswordAuthenticationToken.unauthenticated(
                            request.identifier().trim(), request.password()));

            NexoUserPrincipal principal = (NexoUserPrincipal) authentication.getPrincipal();
            tokenCookieService.write(httpResponse, tokenSessionService.start(principal, metadata));
            loginAttemptService.recordSuccess(request.identifier(), metadata.ipAddress());
            log.info("[NEXO-BACK][AUTH] Login succeeded userId={} ip={}", principal.userId(), metadata.ipAddress());
            return toResponse(principal.userId());
        } catch (BadCredentialsException | DisabledException exception) {
            log.warn("[NEXO-BACK][AUTH] Login rejected identifierType={} ip={} reason={}", identifierType(request.identifier()), metadata.ipAddress(), exception.getClass().getSimpleName());
            loginAttemptService.recordFailure(request.identifier(), metadata.ipAddress());
            tokenSessionService.recordLoginFailure(metadata);
            throw new InvalidCredentialsException();
        }
    }

    public void refresh(HttpServletRequest request, HttpServletResponse response) {
        String refreshToken = tokenCookieService.refreshToken(request)
                .orElseThrow(InvalidRefreshTokenException::new);
        ClientAccessMetadata metadata = clientAccessService.extract(request);
        tokenCookieService.write(response, tokenSessionService.refresh(
                refreshToken, metadata));
        log.info("[NEXO-BACK][AUTH] Session refreshed ip={}", metadata.ipAddress());
    }

    public UserResponse currentUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof NexoUserPrincipal principal)) {
            throw new UnauthenticatedSessionException();
        }
        return toResponse(principal.userId());
    }

    public void logout(HttpServletRequest request, HttpServletResponse response) {
        ClientAccessMetadata metadata = clientAccessService.extract(request);
        tokenCookieService.accessToken(request).ifPresent(accessToken ->
                tokenSessionService.revokeAccessToken(
                        accessToken, metadata));
        tokenCookieService.clear(response);
        log.info("[NEXO-BACK][AUTH] Logout completed ip={}", metadata.ipAddress());
    }

    private UserResponse toResponse(java.util.UUID userId) {
        UserAccount user = userAccountRepository.findById(userId)
                .orElseThrow(UserNotFoundException::new);
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getName(),
                user.getBirthDate(), user.getCreatedAt(), user.getRole());
    }

    private String identifierType(String identifier) {
        return identifier != null && identifier.contains("@") ? "email" : "username";
    }
}
