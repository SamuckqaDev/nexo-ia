package com.nexoia.conversation.controller;

import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.conversation.dto.ConversationMessageResponse;
import com.nexoia.conversation.dto.ConversationResponse;
import com.nexoia.conversation.dto.CreateConversationRequest;
import com.nexoia.conversation.dto.SendMessageRequest;
import com.nexoia.conversation.dto.UpdateConversationModelRequest;
import com.nexoia.conversation.service.ConversationService;
import com.nexoia.shared.api.BaseResponse;
import jakarta.validation.Valid;
import java.util.List; import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController @RequiredArgsConstructor @RequestMapping("/api/v1/conversations")
public class ConversationController {
    private final ConversationService service;
    @GetMapping public ResponseEntity<BaseResponse<ConversationResponse>> list(@AuthenticationPrincipal NexoUserPrincipal principal) { return ResponseEntity.ok(BaseResponse.success(200, "Conversations retrieved", service.list(principal.userId()))); }
    @PostMapping public ResponseEntity<BaseResponse<ConversationResponse>> create(@AuthenticationPrincipal NexoUserPrincipal principal, @Valid @RequestBody CreateConversationRequest request) { return ResponseEntity.status(201).body(BaseResponse.success(201, "Conversation created", service.create(principal.userId(), request))); }
    @GetMapping("/{conversationId}/messages") public ResponseEntity<BaseResponse<ConversationMessageResponse>> messages(@AuthenticationPrincipal NexoUserPrincipal principal, @PathVariable UUID conversationId) { return ResponseEntity.ok(BaseResponse.success(200, "Conversation messages retrieved", service.messages(principal.userId(), conversationId))); }
    @PostMapping("/{conversationId}/messages") public ResponseEntity<BaseResponse<ConversationMessageResponse>> send(@AuthenticationPrincipal NexoUserPrincipal principal, @PathVariable UUID conversationId, @Valid @RequestBody SendMessageRequest request) { return ResponseEntity.status(201).body(BaseResponse.success(201, "Message saved", service.addUserMessage(principal.userId(), conversationId, request.content()))); }
    @PutMapping("/{conversationId}/model") public ResponseEntity<BaseResponse<ConversationResponse>> selectModel(@AuthenticationPrincipal NexoUserPrincipal principal, @PathVariable UUID conversationId, @Valid @RequestBody UpdateConversationModelRequest request) { return ResponseEntity.ok(BaseResponse.success(200, "Conversation model selected", service.selectModel(principal.userId(), conversationId, request))); }
}
