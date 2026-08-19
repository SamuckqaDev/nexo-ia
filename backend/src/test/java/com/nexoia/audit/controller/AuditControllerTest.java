package com.nexoia.audit.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexoia.audit.service.AuditService;
import com.nexoia.auth.access.service.ClientAccessService;
import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.auth.token.security.JwtCookieAuthenticationFilter;
import com.nexoia.auth.token.service.TokenCookieService;
import com.nexoia.auth.token.service.TokenSessionService;
import com.nexoia.auth.user.model.UserRole;
import com.nexoia.shared.exception.GlobalExceptionHandler;
import com.nexoia.shared.security.SecurityConfiguration;
import com.nexoia.shared.security.handler.ApiAccessDeniedHandler;
import com.nexoia.shared.security.handler.ApiAuthenticationEntryPoint;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * The audit trail is administrative. An unauthenticated caller is rejected, a member is denied, and
 * only an Owner may read it, so a member can never inspect another member's recorded activity.
 */
@WebMvcTest(value = AuditController.class, properties =
        "nexo.security.token.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
@Import({SecurityConfiguration.class, GlobalExceptionHandler.class,
        ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class,
        JwtCookieAuthenticationFilter.class})
class AuditControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private AuditService service;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private TokenCookieService tokenCookieService;
    @MockitoBean private TokenSessionService tokenSessionService;
    @MockitoBean private ClientAccessService clientAccessService;

    @Test
    void rejectsAnUnauthenticatedCaller() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void deniesAMember() throws Exception {
        mockMvc.perform(get("/api/v1/admin/audit").with(principal(UserRole.MEMBER)))
                .andExpect(status().isForbidden());
    }

    @Test
    void letsTheOwnerInspectTheTrail() throws Exception {
        when(service.defaultLimit()).thenReturn(100);
        when(service.query(any(), any(), eq(100))).thenReturn(List.of());

        mockMvc.perform(get("/api/v1/admin/audit").with(principal(UserRole.OWNER)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200));
    }

    private RequestPostProcessor principal(UserRole role) {
        NexoUserPrincipal principal = new NexoUserPrincipal(
                UUID.randomUUID(), "actor", "actor@nexo.local", "Actor",
                Instant.parse("2026-08-19T10:00:00Z"), role, "hash", true);

        return authentication(new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()));
    }
}
