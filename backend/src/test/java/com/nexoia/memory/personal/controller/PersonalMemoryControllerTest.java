package com.nexoia.memory.personal.controller;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexoia.auth.access.service.ClientAccessService;
import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.auth.token.security.JwtCookieAuthenticationFilter;
import com.nexoia.auth.token.service.TokenCookieService;
import com.nexoia.auth.token.service.TokenSessionService;
import com.nexoia.auth.user.model.UserRole;
import com.nexoia.memory.personal.dto.PersonalMemoryResponse;
import com.nexoia.memory.personal.service.PersonalMemoryService;
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
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

@WebMvcTest(value = PersonalMemoryController.class, properties =
        "nexo.security.token.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
@Import({SecurityConfiguration.class, GlobalExceptionHandler.class,
        ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class,
        JwtCookieAuthenticationFilter.class})
class PersonalMemoryControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private PersonalMemoryService service;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private TokenCookieService tokenCookieService;
    @MockitoBean private TokenSessionService tokenSessionService;
    @MockitoBean private ClientAccessService clientAccessService;

    private final UUID userId = UUID.randomUUID();
    private final UUID memoryId = UUID.randomUUID();

    @Test
    void requiresAnAuthenticatedSession() throws Exception {
        mockMvc.perform(get("/api/v1/memories"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void listsOnlyMemoriesResolvedForTheAuthenticatedCaller() throws Exception {
        when(service.list(userId)).thenReturn(List.of(memory()));

        mockMvc.perform(get("/api/v1/memories").with(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(memoryId.toString()))
                .andExpect(jsonPath("$.data[0].createdAt").value("2026-08-24T12:00:00Z"));

        verify(service).list(userId);
    }

    @Test
    void createsMemoryUnderTheAuthenticatedCaller() throws Exception {
        when(service.remember(eq(userId), eq("Prefers concise answers"), eq(null), eq(null)))
                .thenReturn(memory());

        mockMvc.perform(post("/api/v1/memories").with(principal()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"Prefers concise answers\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data[0].content").value("Prefers concise answers"));
    }

    @Test
    void removesMemoryThroughTheAuthenticatedOwnerScope() throws Exception {
        mockMvc.perform(delete("/api/v1/memories/{memoryId}", memoryId)
                        .with(principal()).with(csrf()))
                .andExpect(status().isOk());

        verify(service).remove(userId, memoryId);
    }

    private PersonalMemoryResponse memory() {
        Instant timestamp = Instant.parse("2026-08-24T12:00:00Z");
        return new PersonalMemoryResponse(
                memoryId, "Prefers concise answers", null, timestamp, timestamp);
    }

    private RequestPostProcessor principal() {
        NexoUserPrincipal principal = new NexoUserPrincipal(
                userId, "owner", "owner@nexo.local", "Owner",
                Instant.parse("2026-08-18T12:00:00Z"), UserRole.OWNER, "hash", true);
        return authentication(new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()));
    }
}
