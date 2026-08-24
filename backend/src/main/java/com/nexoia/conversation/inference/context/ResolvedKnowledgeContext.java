package com.nexoia.conversation.inference.context;

import com.nexoia.knowledge.retrieval.dto.CitationResponse;
import java.util.List;

/** Citations plus the honest terminal state of one Knowledge Vault retrieval attempt. */
public record ResolvedKnowledgeContext(
        List<CitationResponse> citations,
        KnowledgeSearchStatus status) {

    public ResolvedKnowledgeContext {
        citations = List.copyOf(citations);
    }

    public static ResolvedKnowledgeContext notRequested() {
        return new ResolvedKnowledgeContext(List.of(), KnowledgeSearchStatus.NOT_REQUESTED);
    }

    public static ResolvedKnowledgeContext availableOnDemand() {
        return new ResolvedKnowledgeContext(List.of(), KnowledgeSearchStatus.AVAILABLE_ON_DEMAND);
    }

    public static ResolvedKnowledgeContext unavailable() {
        return new ResolvedKnowledgeContext(List.of(), KnowledgeSearchStatus.UNAVAILABLE);
    }
}
