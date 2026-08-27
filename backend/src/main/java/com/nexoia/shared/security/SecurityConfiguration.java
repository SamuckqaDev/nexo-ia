package com.nexoia.shared.security;

import com.nexoia.auth.token.config.TokenProperties;
import com.nexoia.auth.token.security.JwtCookieAuthenticationFilter;
import com.nexoia.shared.security.handler.ApiAccessDeniedHandler;
import com.nexoia.shared.security.handler.ApiAuthenticationEntryPoint;
import jakarta.servlet.DispatcherType;
import java.time.Clock;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.ProviderManager;
import org.springframework.security.authentication.dao.DaoAuthenticationProvider;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.password.Pbkdf2PasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
@EnableMethodSecurity
@EnableConfigurationProperties(TokenProperties.class)
public class SecurityConfiguration {

    @Bean
    SecurityFilterChain applicationSecurity(
            HttpSecurity http,
            JwtCookieAuthenticationFilter jwtCookieAuthenticationFilter,
            ApiAuthenticationEntryPoint authenticationEntryPoint,
            ApiAccessDeniedHandler accessDeniedHandler) throws Exception {
        return http
                .authorizeHttpRequests(authorize -> authorize
                        .dispatcherTypeMatchers(DispatcherType.ASYNC, DispatcherType.ERROR)
                        .permitAll()
                        .requestMatchers(
                                "/api/v1/system",
                                "/api/v1/auth/csrf",
                                "/api/v1/auth/login",
                                "/api/v1/auth/refresh",
                                "/api/v1/auth/password/forgot",
                                "/api/v1/auth/password/reset",
                                "/api/v1/auth/bootstrap/**",
                                "/api/v1/device-runtime/pair",
                                "/api/v1/device-runtime/workspaces/**",
                                "/api/v1/device-runtime/connect",
                                "/actuator/health")
                        .permitAll()
                        .anyRequest().authenticated())
                .csrf(csrf -> csrf
                        .csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                        .ignoringRequestMatchers(
                                "/api/v1/device-runtime/pair",
                                "/api/v1/device-runtime/workspaces/**")
                        .spa())
                .exceptionHandling(exceptions -> exceptions
                        .authenticationEntryPoint(authenticationEntryPoint)
                        .accessDeniedHandler(accessDeniedHandler))
                .addFilterBefore(jwtCookieAuthenticationFilter, UsernamePasswordAuthenticationFilter.class)
                .build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return Pbkdf2PasswordEncoder.defaultsForSpringSecurity_v5_8();
    }

    @Bean
    AuthenticationManager authenticationManager(
            UserDetailsService userDetailsService,
            PasswordEncoder passwordEncoder) {
        DaoAuthenticationProvider provider = new DaoAuthenticationProvider(userDetailsService);
        provider.setPasswordEncoder(passwordEncoder);
        return new ProviderManager(provider);
    }

    @Bean
    Clock applicationClock() {
        return Clock.systemUTC();
    }
}
