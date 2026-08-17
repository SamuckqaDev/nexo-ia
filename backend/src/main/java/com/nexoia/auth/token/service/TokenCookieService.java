package com.nexoia.auth.token.service;

import com.nexoia.auth.token.config.TokenProperties;
import com.nexoia.auth.token.dto.IssuedTokenPair;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.time.Duration;
import java.util.Arrays;
import java.util.Optional;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Service;

@Service
public class TokenCookieService {

    public static final String ACCESS_COOKIE = "NEXO_ACCESS";
    public static final String REFRESH_COOKIE = "NEXO_REFRESH";

    private final TokenProperties properties;

    public TokenCookieService(TokenProperties properties) {
        this.properties = properties;
    }

    public void write(HttpServletResponse response, IssuedTokenPair tokens) {
        add(response, ACCESS_COOKIE, tokens.accessToken(), properties.accessTtl(), "/");
        add(response, REFRESH_COOKIE, tokens.refreshToken(), properties.refreshTtl(),
                "/api/v1/auth/refresh");
    }

    public void clear(HttpServletResponse response) {
        add(response, ACCESS_COOKIE, "", Duration.ZERO, "/");
        add(response, REFRESH_COOKIE, "", Duration.ZERO, "/api/v1/auth/refresh");
    }

    public Optional<String> accessToken(HttpServletRequest request) {
        return cookie(request, ACCESS_COOKIE);
    }

    public Optional<String> refreshToken(HttpServletRequest request) {
        return cookie(request, REFRESH_COOKIE);
    }

    private Optional<String> cookie(HttpServletRequest request, String name) {
        if (request.getCookies() == null) {
            return Optional.empty();
        }
        return Arrays.stream(request.getCookies())
                .filter(cookie -> name.equals(cookie.getName()))
                .map(Cookie::getValue)
                .filter(value -> !value.isBlank())
                .findFirst();
    }

    private void add(HttpServletResponse response, String name, String value, Duration maxAge,
            String path) {
        ResponseCookie cookie = ResponseCookie.from(name, value)
                .httpOnly(true)
                .secure(properties.secureCookie())
                .sameSite("Lax")
                .path(path)
                .maxAge(maxAge)
                .build();
        response.addHeader(HttpHeaders.SET_COOKIE, cookie.toString());
    }
}
