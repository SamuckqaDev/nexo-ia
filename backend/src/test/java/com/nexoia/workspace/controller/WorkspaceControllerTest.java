package com.nexoia.workspace.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
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
import com.nexoia.shared.exception.GlobalExceptionHandler;
import com.nexoia.shared.security.SecurityConfiguration;
import com.nexoia.shared.security.handler.ApiAccessDeniedHandler;
import com.nexoia.shared.security.handler.ApiAuthenticationEntryPoint;
import com.nexoia.workspace.dto.WorkspaceResponse;
import com.nexoia.workspace.dto.WorkspaceStatusResponse;
import com.nexoia.workspace.exception.WorkspaceNotFoundException;
import com.nexoia.workspace.model.WorkspaceAccessMode;
import com.nexoia.workspace.model.WorkspaceStatus;
import com.nexoia.workspace.model.WorkspaceStorageType;
import com.nexoia.workspace.service.WorkspaceService;
import java.time.Instant;
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
 * Confirms the workspace endpoints require authentication, derive the caller from the principal, and
 * translate {@link WorkspaceNotFoundException} into a 404 so one user can never learn whether another
 * user's workspace exists.
 */
@WebMvcTest(value = WorkspaceController.class, properties =
        "nexo.security.token.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
@Import({SecurityConfiguration.class, GlobalExceptionHandler.class,
        ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class,
        JwtCookieAuthenticationFilter.class})
class WorkspaceControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private WorkspaceService service;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private TokenCookieService tokenCookieService;
    @MockitoBean private TokenSessionService tokenSessionService;
    @MockitoBean private ClientAccessService clientAccessService;

    private final UUID userId = UUID.randomUUID();
    private final UUID workspaceId = UUID.randomUUID();

    private RequestPostProcessor principal() {
        NexoUserPrincipal user = new NexoUserPrincipal(
                userId, "sam", "sam@nexo.dev", "Sam", Instant.now(), UserRole.MEMBER, "x", true);
        return authentication(new UsernamePasswordAuthenticationToken(user, null, user.getAuthorities()));
    }

    @Test
    void requiresAnAuthenticatedSessionToList() throws Exception {
        mockMvc.perform(get("/api/v1/workspaces"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void returnsWorkspaceForOwner() throws Exception {
        when(service.get(eq(userId), eq(workspaceId))).thenReturn(new WorkspaceResponse(
                workspaceId, "proj", WorkspaceStorageType.MOUNTED, WorkspaceAccessMode.READ_ONLY,
                WorkspaceStatus.AVAILABLE, "project", Instant.now(), Instant.now(), Instant.now()));

        mockMvc.perform(get("/api/v1/workspaces/{id}", workspaceId).with(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(workspaceId.toString()))
                .andExpect(jsonPath("$.data[0].status").value("AVAILABLE"));
    }

    @Test
    void hidesForeignWorkspaceAsNotFound() throws Exception {
        when(service.status(eq(userId), any())).thenThrow(new WorkspaceNotFoundException());

        mockMvc.perform(get("/api/v1/workspaces/{id}/status", workspaceId).with(principal()))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void rejectsBindingWithoutCsrf() throws Exception {
        mockMvc.perform(post("/api/v1/workspaces/{id}/refresh", workspaceId).with(principal()))
                .andExpect(status().isForbidden());
    }

    @Test
    void refreshesWorkspaceWithCsrf() throws Exception {
        when(service.refresh(eq(userId), eq(workspaceId))).thenReturn(new WorkspaceStatusResponse(
                WorkspaceStatus.AVAILABLE, WorkspaceStorageType.MOUNTED, WorkspaceAccessMode.READ_ONLY,
                "project", "fingerprint", Instant.now(), null, java.util.List.of(), null));

        mockMvc.perform(post("/api/v1/workspaces/{id}/refresh", workspaceId).with(principal()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].status").value("AVAILABLE"));
    }
}
