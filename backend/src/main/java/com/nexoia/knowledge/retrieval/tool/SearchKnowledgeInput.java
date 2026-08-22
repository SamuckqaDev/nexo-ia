package com.nexoia.knowledge.retrieval.tool;

/** Model-facing input. Authorization scope is intentionally absent. */
public record SearchKnowledgeInput(String query, Integer limit) {}
