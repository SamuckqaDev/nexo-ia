package com.nexoia.knowledge.vault.model;

/**
 * The authorization scope of a Knowledge Vault. {@code PROJECT}, {@code TEAM}, and {@code ORGANIZATION}
 * are part of the contract but always rejected by {@code KnowledgeVaultService} — no backend
 * project/team/organization entity exists yet to authorize against. See D-026.
 */
public enum VaultScope {
    PERSONAL,
    WORKSPACE,
    PROJECT,
    TEAM,
    ORGANIZATION
}
