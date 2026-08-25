package com.nexoia.permission.model;

/**
 * Every effect Nexo could cause belongs to one family with a fixed {@link RiskClass} and the minimum
 * {@link UnlockLevel} that unlocks it. The family — never an individual model request — carries the
 * rules. Content and topic are deliberately absent: they are the separate {@link ContentStance} axis.
 *
 * <p>See {@code docs/PERMISSION_PROFILES.md} for the full catalogue.
 */
public enum CapabilityFamily {

    /** Answer, explain, translate, summarize, draft. Text only. */
    INFERENCE(UnlockLevel.L0_OBSERVER, RiskClass.NONE, false, false),

    /** {@code search_knowledge} over the user's own selected Vaults. */
    KNOWLEDGE_READ(UnlockLevel.L1_GROUNDED, RiskClass.LOW, false, false),

    /** {@code save_to_vault}: append embedded knowledge into a writable Vault for future retrieval. */
    KNOWLEDGE_WRITE(UnlockLevel.L1_GROUNDED, RiskClass.MEDIUM, true, false),

    /** {@code remember}: store or recall the user's own short personal notes. */
    PERSONAL_MEMORY(UnlockLevel.L1_GROUNDED, RiskClass.LOW, false, false),

    /** {@code update_plan}, {@code inspect_capabilities}: internal visible plan, no external effect. */
    PLANNING(UnlockLevel.L2_CONNECTED, RiskClass.LOW, false, false),

    /** Enabled read/open-world {@code mcp_*} tools (fetch, web search). */
    EXTERNAL_READ(UnlockLevel.L2_CONNECTED, RiskClass.MEDIUM, true, false),

    /** Deferred. Write/destructive {@code mcp_*} tools. */
    EXTERNAL_WRITE(UnlockLevel.L4_BUILDER, RiskClass.HIGH, true, false),

    /** Deferred. Read files and repository metadata in an attached Workspace. */
    WORKSPACE_READ(UnlockLevel.L3_WORKSPACE_READER, RiskClass.MEDIUM, true, false),

    /** Deferred. Edit files, stage or commit Git in an authorized Workspace. */
    WORKSPACE_WRITE(UnlockLevel.L4_BUILDER, RiskClass.HIGH, true, false),

    /** Deferred. Terminal, process, browser, desktop through typed OS capabilities. */
    SYSTEM_CONTROL(UnlockLevel.L5_OPERATOR, RiskClass.CRITICAL, false, false),

    /** Read or emit tokens, keys, passwords, or financial data. Never exposed to the model. */
    SECRETS(UnlockLevel.L5_OPERATOR, RiskClass.PROHIBITED, false, true);

    private final UnlockLevel minLevel;
    private final RiskClass risk;
    private final boolean requiresTarget;
    private final boolean prohibited;

    CapabilityFamily(UnlockLevel minLevel, RiskClass risk, boolean requiresTarget, boolean prohibited) {
        this.minLevel = minLevel;
        this.risk = risk;
        this.requiresTarget = requiresTarget;
        this.prohibited = prohibited;
    }

    public UnlockLevel minLevel() {
        return minLevel;
    }

    public RiskClass risk() {
        return risk;
    }

    /**
     * Whether the family needs an authorized target resolved outside the model (a writable Vault, an
     * enabled MCP connection, an attached Workspace) before it can be allowed.
     */
    public boolean requiresTarget() {
        return requiresTarget;
    }

    /** Whether the family is never exposed regardless of level or profile. */
    public boolean prohibited() {
        return prohibited;
    }

    /** A human label for the capability envelope, for example {@code "knowledge write"}. */
    public String label() {
        return name().replace('_', ' ').toLowerCase();
    }
}
