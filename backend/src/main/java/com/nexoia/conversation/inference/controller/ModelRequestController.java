package com.nexoia.conversation.inference.controller;

import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.conversation.chat.dto.SendMessageRequest;
import com.nexoia.conversation.inference.config.ModelStreamProperties;
import com.nexoia.conversation.inference.dto.ModelRequestReservation;
import com.nexoia.conversation.inference.service.ModelRequestService;
import com.nexoia.shared.api.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ExecutorService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/conversations")
public class ModelRequestController {

    private final ModelRequestService service;
    private final ExecutorService modelRequestExecutor;
    private final ModelStreamProperties properties;

    /**
     * Sends a message to the conversation's selected model and streams the ordered response events.
     *
     * <p>SSE is a deliberate exception to the {@code BaseResponse} envelope because the response
     * stays open. The request is reserved before the emitter is created, so a missing conversation,
     * an unselected model, or a conversation that is already generating still receives a normal
     * error response.
     */
    @PostMapping("/{conversationId}/messages/stream")
    @Operation(summary = "Stream a model answer for a new user message")
    public ResponseEntity<SseEmitter> stream(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID conversationId,
            @Valid @RequestBody SendMessageRequest request) {
        ModelRequestReservation reservation =
                service.begin(principal.userId(), conversationId, request.content(), request.thinkingEnabled());
        SseEmitter emitter = new SseEmitter(properties.timeout().toMillis());
        UUID messageId = reservation.assistantMessageId();

        modelRequestExecutor.execute(() -> service.run(
                reservation, new SseModelStreamListener(emitter, messageId)));

        return ResponseEntity.ok(emitter);
    }

    @PostMapping("/{conversationId}/messages/{messageId}/cancel")
    @Operation(summary = "Cancel the model request in progress for this conversation")
    public ResponseEntity<BaseResponse<Void>> cancel(
            @AuthenticationPrincipal NexoUserPrincipal principal,
            @PathVariable UUID conversationId,
            @PathVariable UUID messageId) {
        service.cancel(principal.userId(), conversationId, messageId);

        return ResponseEntity.ok(BaseResponse.success(200, "Model request cancelling", List.of()));
    }
}
