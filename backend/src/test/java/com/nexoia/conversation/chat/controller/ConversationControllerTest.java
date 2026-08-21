package com.nexoia.conversation.chat.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexoia.auth.access.service.ClientAccessService;
import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.auth.token.security.JwtCookieAuthenticationFilter;
import com.nexoia.auth.token.service.TokenCookieService;
import com.nexoia.auth.token.service.TokenSessionService;
import com.nexoia.auth.user.model.UserRole;
import com.nexoia.conversation.chat.dto.ConversationResponse;
import com.nexoia.conversation.chat.exception.ConversationNotFoundException;
import com.nexoia.conversation.chat.service.ConversationService;
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

/**
 * Confirms the conversation endpoints require authentication, extract the caller from the
 * principal rather than a request field, and translate {@link ConversationNotFoundException} into
 * a 404 so one user can never learn whether another user's conversation exists.
 */
@WebMvcTest(value = ConversationController.class, properties =
        "nexo.security.token.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
@Import({SecurityConfiguration.class, GlobalExceptionHandler.class,
        ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class,
        JwtCookieAuthenticationFilter.class})
class ConversationControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ConversationService service;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private TokenCookieService tokenCookieService;
    @MockitoBean private TokenSessionService tokenSessionService;
    @MockitoBean private ClientAccessService clientAccessService;

    private final UUID userId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();

    @Test
    void requiresAnAuthenticatedSessionToList() throws Exception {
        mockMvc.perform(get("/api/v1/conversations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void listsOnlyTheAuthenticatedCallersConversations() throws Exception {
        when(service.list(userId)).thenReturn(List.of(new ConversationResponse(
                conversationId, "First chat", null, null,
                Instant.parse("2026-08-20T12:00:00Z"), Instant.parse("2026-08-20T12:00:00Z"))));

        mockMvc.perform(get("/api/v1/conversations").with(principal()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data[0].id").value(conversationId.toString()));

        verify(service).list(userId);
    }

    @Test
    void createsAConversationOwnedByTheAuthenticatedCaller() throws Exception {
        when(service.create(eq(userId), any())).thenReturn(new ConversationResponse(
                conversationId, "New chat", null, null,
                Instant.parse("2026-08-20T12:00:00Z"), Instant.parse("2026-08-20T12:00:00Z")));

        mockMvc.perform(post("/api/v1/conversations").with(principal()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"New chat\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.data[0].title").value("New chat"));
    }

    @Test
    void rejectsABlankTitleBeforeReachingTheService() throws Exception {
        mockMvc.perform(post("/api/v1/conversations").with(principal()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"  \"}"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void answersNotFoundWhenRenamingAConversationTheCallerDoesNotOwn() throws Exception {
        when(service.rename(eq(userId), eq(conversationId), any()))
                .thenThrow(new ConversationNotFoundException());

        mockMvc.perform(put("/api/v1/conversations/{id}", conversationId)
                        .with(principal()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"title\":\"Renamed\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Conversation not found"));
    }

    @Test
    void archivesAConversationOwnedByTheCaller() throws Exception {
        mockMvc.perform(delete("/api/v1/conversations/{id}", conversationId)
                        .with(principal()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data").isArray());

        verify(service).archive(userId, conversationId);
    }

    @Test
    void answersNotFoundWhenArchivingAConversationTheCallerDoesNotOwn() throws Exception {
        doThrow(new ConversationNotFoundException()).when(service).archive(userId, conversationId);

        mockMvc.perform(delete("/api/v1/conversations/{id}", conversationId)
                        .with(principal()).with(csrf()))
                .andExpect(status().isNotFound());
    }

    @Test
    void answersNotFoundWhenListingMessagesForAConversationTheCallerDoesNotOwn() throws Exception {
        when(service.messages(userId, conversationId)).thenThrow(new ConversationNotFoundException());

        mockMvc.perform(get("/api/v1/conversations/{id}/messages", conversationId).with(principal()))
                .andExpect(status().isNotFound());
    }

    private RequestPostProcessor principal() {
        NexoUserPrincipal principal = new NexoUserPrincipal(
                userId, "owner", "owner@nexo.local", "Owner",
                Instant.parse("2026-08-18T12:00:00Z"), UserRole.OWNER, "hash", true);

        return authentication(new UsernamePasswordAuthenticationToken(
                principal, null, principal.getAuthorities()));
    }
}
