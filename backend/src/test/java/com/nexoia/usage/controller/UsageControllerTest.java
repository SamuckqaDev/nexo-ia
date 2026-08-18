package com.nexoia.usage.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexoia.auth.access.service.ClientAccessService;
import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.auth.token.security.JwtCookieAuthenticationFilter;
import com.nexoia.auth.token.service.TokenCookieService;
import com.nexoia.auth.token.service.TokenSessionService;
import com.nexoia.auth.user.model.UserRole;
import com.nexoia.provider.model.ProcessingLocation;
import com.nexoia.shared.exception.GlobalExceptionHandler;
import com.nexoia.shared.security.SecurityConfiguration;
import com.nexoia.shared.security.handler.ApiAccessDeniedHandler;
import com.nexoia.shared.security.handler.ApiAuthenticationEntryPoint;
import com.nexoia.usage.dto.UsageLocationBreakdown;
import com.nexoia.usage.dto.UsageSummaryResponse;
import com.nexoia.usage.dto.UsageTotals;
import com.nexoia.usage.model.UsagePeriod;
import com.nexoia.usage.service.UsageService;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(value = UsageController.class, properties =
        "nexo.security.token.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
@Import({SecurityConfiguration.class, GlobalExceptionHandler.class,
        ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class,
        JwtCookieAuthenticationFilter.class})
class UsageControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private UsageService service;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private TokenCookieService tokenCookieService;
    @MockitoBean private TokenSessionService tokenSessionService;
    @MockitoBean private ClientAccessService clientAccessService;

    private final UUID userId = UUID.randomUUID();

    @Test
    void requiresAnAuthenticatedSession() throws Exception {
        mockMvc.perform(get("/api/v1/usage"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void defaultsToTheSevenDayWindowForTheAuthenticatedMember() throws Exception {
        when(service.summary(eq(userId), eq(UsagePeriod.LAST_7_DAYS))).thenReturn(summary());

        mockMvc.perform(get("/api/v1/usage").with(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data[0].period").value("LAST_7_DAYS"))
                .andExpect(jsonPath("$.data[0].totals.totalTokens").value(60));
    }

    @Test
    void honoursTheRequestedPeriod() throws Exception {
        when(service.summary(eq(userId), eq(UsagePeriod.LAST_30_DAYS))).thenReturn(summary());

        mockMvc.perform(get("/api/v1/usage").param("period", "LAST_30_DAYS").with(principal()))
                .andExpect(status().isOk());
    }

    @Test
    void rejectsAnUnknownPeriod() throws Exception {
        mockMvc.perform(get("/api/v1/usage").param("period", "LAST_YEAR").with(principal()))
                .andExpect(status().isBadRequest());
    }

    private UsageSummaryResponse summary() {
        return new UsageSummaryResponse(
                UsagePeriod.LAST_7_DAYS,
                Instant.parse("2026-08-11T00:00:00Z"),
                Instant.parse("2026-08-18T00:00:00Z"),
                new UsageTotals(3, 2, 1, 0, 50, 10, 60, 1500.0, 0),
                List.of(),
                List.of(),
                List.of(new UsageLocationBreakdown(ProcessingLocation.LOCAL, 3, 60)));
    }

    private RequestPostProcessor principal() {
        NexoUserPrincipal principal = new NexoUserPrincipal(
                userId, "owner", "owner@nexo.local", "Owner",
                Instant.parse("2026-08-18T12:00:00Z"), UserRole.OWNER, "hash", true);

        return authentication(new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()));
    }
}
