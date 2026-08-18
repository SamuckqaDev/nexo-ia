package com.nexoia.conversation.inference.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nexoia.auth.access.service.ClientAccessService;
import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.auth.token.security.JwtCookieAuthenticationFilter;
import com.nexoia.auth.token.service.TokenCookieService;
import com.nexoia.auth.token.service.TokenSessionService;
import com.nexoia.auth.user.model.UserRole;
import com.nexoia.conversation.chat.exception.ConversationBusyException;
import com.nexoia.conversation.chat.exception.ConversationNotFoundException;
import com.nexoia.conversation.inference.config.ModelStreamProperties;
import com.nexoia.conversation.inference.exception.ModelNotSelectedException;
import com.nexoia.conversation.inference.service.ModelRequestRegistry;
import com.nexoia.conversation.inference.service.ModelRequestService;
import com.nexoia.shared.exception.GlobalExceptionHandler;
import com.nexoia.shared.security.SecurityConfiguration;
import com.nexoia.shared.security.handler.ApiAccessDeniedHandler;
import com.nexoia.shared.security.handler.ApiAuthenticationEntryPoint;
import java.time.Duration;
import java.time.Instant;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.request.RequestPostProcessor;

/**
 * The streaming endpoint must still answer with the normal envelope while the response has not been
 * committed. Once events start flowing the status can no longer change, so every validation failure
 * has to surface here.
 */
@WebMvcTest(value = ModelRequestController.class, properties =
        "nexo.security.token.secret=AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA=")
@Import({SecurityConfiguration.class, GlobalExceptionHandler.class,
        ApiAuthenticationEntryPoint.class, ApiAccessDeniedHandler.class,
        JwtCookieAuthenticationFilter.class, ModelRequestControllerTest.StreamTestConfiguration.class})
class ModelRequestControllerTest {

    @Autowired private MockMvc mockMvc;
    @MockitoBean private ModelRequestService service;
    @MockitoBean private ModelRequestRegistry registry;
    @MockitoBean private ExecutorService modelRequestExecutor;
    @MockitoBean private UserDetailsService userDetailsService;
    @MockitoBean private TokenCookieService tokenCookieService;
    @MockitoBean private TokenSessionService tokenSessionService;
    @MockitoBean private ClientAccessService clientAccessService;

    private final UUID userId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();

    @TestConfiguration
    static class StreamTestConfiguration {
        @Bean
        ModelStreamProperties modelStreamProperties() {
            return new ModelStreamProperties(Duration.ofMinutes(10));
        }
    }

    @Test
    void requiresAnAuthenticatedSessionToStream() throws Exception {
        mockMvc.perform(post("/api/v1/conversations/{id}/messages/stream", conversationId)
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hello\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    void answersNotFoundForAConversationTheCallerDoesNotOwn() throws Exception {
        when(service.begin(eq(userId), eq(conversationId), any()))
                .thenThrow(new ConversationNotFoundException());

        mockMvc.perform(post("/api/v1/conversations/{id}/messages/stream", conversationId)
                        .with(principal()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hello\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(404))
                .andExpect(jsonPath("$.message").value("Conversation not found"));
    }

    @Test
    void answersConflictWhenTheConversationIsAlreadyGenerating() throws Exception {
        when(service.begin(eq(userId), eq(conversationId), any()))
                .thenThrow(new ConversationBusyException());

        mockMvc.perform(post("/api/v1/conversations/{id}/messages/stream", conversationId)
                        .with(principal()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hello\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value(409));
    }

    @Test
    void answersUnprocessableWhenNoModelIsSelected() throws Exception {
        when(service.begin(eq(userId), eq(conversationId), any()))
                .thenThrow(new ModelNotSelectedException());

        mockMvc.perform(post("/api/v1/conversations/{id}/messages/stream", conversationId)
                        .with(principal()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"hello\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.code").value(422));
    }

    @Test
    void rejectsAnEmptyMessageBeforeReachingTheService() throws Exception {
        mockMvc.perform(post("/api/v1/conversations/{id}/messages/stream", conversationId)
                        .with(principal()).with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"content\":\"   \"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    void cancelsTheRunningRequestForItsOwner() throws Exception {
        UUID messageId = UUID.randomUUID();

        mockMvc.perform(post("/api/v1/conversations/{id}/messages/{messageId}/cancel",
                        conversationId, messageId)
                        .with(principal()).with(csrf()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.message").value("Model request cancelling"));

        verify(service).cancel(userId, conversationId, messageId);
    }

    @Test
    void reportsNoRunningRequestWhenCancellationArrivesTooLate() throws Exception {
        UUID messageId = UUID.randomUUID();
        doThrow(new com.nexoia.conversation.inference.exception.ModelRequestNotFoundException())
                .when(service).cancel(userId, conversationId, messageId);

        mockMvc.perform(post("/api/v1/conversations/{id}/messages/{messageId}/cancel",
                        conversationId, messageId)
                        .with(principal()).with(csrf()))
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
