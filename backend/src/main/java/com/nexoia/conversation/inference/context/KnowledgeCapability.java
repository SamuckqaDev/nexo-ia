package com.nexoia.conversation.inference.context;

import java.util.List;

/**
 * What Knowledge Vault access this request actually produced. The explicit search status and source
 * count prevent the model from claiming a successful lookup when none happened.
 */
public record KnowledgeCapability(
        List<String> selectedVaultNames,
        int selectedVaultCount,
        KnowledgeSearchStatus searchStatus,
        int sourcesRetrieved,
        List<ContextSourceSummary> sources) {

    public static KnowledgeCapability none() {
        return new KnowledgeCapability(
                List.of(), 0, KnowledgeSearchStatus.NOT_REQUESTED, 0, List.of());
    }
}
