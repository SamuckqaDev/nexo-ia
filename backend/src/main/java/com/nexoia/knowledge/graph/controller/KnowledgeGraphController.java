package com.nexoia.knowledge.graph.controller;

import com.nexoia.auth.session.security.NexoUserPrincipal;
import com.nexoia.knowledge.graph.dto.KnowledgeGraphResponse;
import com.nexoia.knowledge.graph.service.KnowledgeGraphService;
import com.nexoia.shared.api.BaseResponse;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/knowledge/graph")
public class KnowledgeGraphController {

    private final KnowledgeGraphService service;

    @GetMapping
    @Operation(summary = "Build the authenticated user's bounded semantic Knowledge Vault graph")
    public ResponseEntity<BaseResponse<KnowledgeGraphResponse>> graph(
            @AuthenticationPrincipal NexoUserPrincipal principal) {
        return ResponseEntity.ok(BaseResponse.success(
                200, "Knowledge graph retrieved", service.graph(principal.userId())));
    }
}
