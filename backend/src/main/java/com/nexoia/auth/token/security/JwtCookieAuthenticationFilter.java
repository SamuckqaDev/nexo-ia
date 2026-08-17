package com.nexoia.auth.token.security;

import com.nexoia.auth.access.service.ClientAccessService;
import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.auth.token.exception.InvalidAccessTokenException;
import com.nexoia.auth.token.service.TokenCookieService;
import com.nexoia.auth.token.service.TokenSessionService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

@Component
@RequiredArgsConstructor
public class JwtCookieAuthenticationFilter extends OncePerRequestFilter {

    private final TokenCookieService tokenCookieService;
    private final TokenSessionService tokenSessionService;
    private final ClientAccessService clientAccessService;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response,
            FilterChain filterChain) throws ServletException, IOException {
        tokenCookieService.accessToken(request).ifPresent(accessToken -> {
            try {
                NexoUserPrincipal principal = tokenSessionService.authenticate(
                        accessToken, clientAccessService.extract(request));
                SecurityContext context = SecurityContextHolder.createEmptyContext();
                context.setAuthentication(UsernamePasswordAuthenticationToken.authenticated(
                        principal, null, principal.getAuthorities()));
                SecurityContextHolder.setContext(context);
            } catch (InvalidAccessTokenException exception) {
                SecurityContextHolder.clearContext();
            }
        });
        filterChain.doFilter(request, response);
    }
}
