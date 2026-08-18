package com.nexoia.conversation.chat.controller;

import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.conversation.chat.dto.ConversationMessageResponse;
import com.nexoia.conversation.chat.dto.ConversationResponse;
import com.nexoia.conversation.chat.dto.CreateConversationRequest;
import com.nexoia.conversation.chat.dto.RenameConversationRequest;
import com.nexoia.conversation.chat.dto.SendMessageRequest;
import com.nexoia.conversation.chat.dto.UpdateConversationModelRequest;
import com.nexoia.conversation.chat.service.ConversationService;
import com.nexoia.shared.api.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/conversations")
public class ConversationController {

    private final ConversationService service;

    @GetMapping
    @Operation(summary = "List the authenticated user's active conversations")
    public ResponseEntity<BaseResponse<ConversationResponse>> list(
            @AuthenticationPrincipal NexoUserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(
                200, "Conversations retrieved", service.list(principal.userId())));
    }

    @PostMapping
    @Operation(summary = "Create a private conversation for the authenticated user")
    public ResponseEntity<BaseResponse<ConversationResponse>> create(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @Valid @RequestBody CreateConversationRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                201, "Conversation created", service.create(principal.userId(), request)));
    }

    @PutMapping("/{conversationId}")
    @Operation(summary = "Rename a conversation owned by the authenticated user")
    public ResponseEntity<BaseResponse<ConversationResponse>> rename(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID conversationId,
            @Valid @RequestBody RenameConversationRequest request) {
        return ResponseEntity.ok(BaseResponse.success(
                200, "Conversation renamed", service.rename(principal.userId(), conversationId, request)));
    }

    @DeleteMapping("/{conversationId}")
    @Operation(summary = "Archive a conversation owned by the authenticated user")
    public ResponseEntity<BaseResponse<Void>> archive(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID conversationId) {
        service.archive(principal.userId(), conversationId);

        return ResponseEntity.ok(BaseResponse.success(200, "Conversation archived", List.of()));
    }

    @PutMapping("/{conversationId}/model")
    @Operation(summary = "Select the provider and model used by a conversation")
    public ResponseEntity<BaseResponse<ConversationResponse>> selectModel(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID conversationId,
            @Valid @RequestBody UpdateConversationModelRequest request) {
        return ResponseEntity.ok(BaseResponse.success(
                200, "Conversation model selected",
                service.selectModel(principal.userId(), conversationId, request)));
    }

    @GetMapping("/{conversationId}/messages")
    @Operation(summary = "List the ordered messages of a conversation")
    public ResponseEntity<BaseResponse<ConversationMessageResponse>> messages(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID conversationId) {
        return ResponseEntity.ok(BaseResponse.success(
                200, "Conversation messages retrieved",
                service.messages(principal.userId(), conversationId)));
    }

    @PostMapping("/{conversationId}/messages")
    @Operation(summary = "Append a user message without requesting model inference")
    public ResponseEntity<BaseResponse<ConversationMessageResponse>> send(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(BaseResponse.success(
                201, "Message saved",
                service.addUserMessage(principal.userId(), conversationId, request.content())));
    }
}
