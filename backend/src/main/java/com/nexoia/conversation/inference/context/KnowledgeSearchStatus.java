package com.nexoia.conversation.inference.context;

/** The truthful result of the deterministic Knowledge Vault retrieval stage. */
public enum KnowledgeSearchStatus {
    NOT_REQUESTED,
    AVAILABLE_ON_DEMAND,
    COMPLETED,
    UNAVAILABLE
}
