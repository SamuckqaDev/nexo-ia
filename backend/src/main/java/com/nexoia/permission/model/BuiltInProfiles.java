package com.nexoia.permission.model;

import java.util.EnumMap;
import java.util.Map;

/**
 * The permission profiles Nexo ships with. They are deterministic presets, not user data, so they live
 * as code until per-user/org customization needs persistence. Names match
 * {@code docs/PERMISSION_PROFILES.md} section 5, and each carries the seed content matrix defined in
 * {@code docs/ORGANIZATIONS_AND_GOVERNANCE.md} section 6 (tunable by root in a later phase).
 */
public final class BuiltInProfiles {

    private BuiltInProfiles() {
    }

    /** Full on trusted areas; graded on the most sensitive ones — the default for a regular member. */
    private static final ContentMatrix GRADED_MATRIX = gradedMatrix();
    private static final ContentMatrix OPEN_MATRIX = ContentMatrix.uniform(ContentAllowance.FULL);
    private static final ContentMatrix CLOSED_MATRIX = ContentMatrix.uniform(ContentAllowance.BLOCK);

    /** Resolves a built-in profile from its stable key — the value persisted as a user's assignment. */
    public static PermissionProfile of(ProfileKey key) {
        return switch (key) {
            case LOCKED -> locked();
            case READER -> reader();
            case RESEARCHER -> researcher();
            case BUILDER -> builder();
            case OPERATOR -> operator();
        };
    }

    /** Safe mode: read-only inference only. */
    public static PermissionProfile locked() {
        return new PermissionProfile("Locked", UnlockLevel.L0_OBSERVER, false, Map.of(), CLOSED_MATRIX);
    }

    /** Grounded Q&A that may also grow the user's own knowledge and memory. */
    public static PermissionProfile reader() {
        return new PermissionProfile("Reader", UnlockLevel.L1_GROUNDED, false, Map.of(), GRADED_MATRIX);
    }

    /** Web/MCP research with citations. */
    public static PermissionProfile researcher() {
        return new PermissionProfile("Researcher", UnlockLevel.L2_CONNECTED, false, Map.of(), GRADED_MATRIX);
    }

    /** Deferred capabilities. Scoped project changes, every write behind approval. */
    public static PermissionProfile builder() {
        return new PermissionProfile("Builder", UnlockLevel.L4_BUILDER, true, Map.of(
                CapabilityFamily.EXTERNAL_WRITE, CapabilityDecision.REQUIRES_APPROVAL,
                CapabilityFamily.WORKSPACE_WRITE, CapabilityDecision.REQUIRES_APPROVAL), OPEN_MATRIX);
    }

    /** Deferred capabilities. Power user on their own machine; system control behind fresh confirmation. */
    public static PermissionProfile operator() {
        return new PermissionProfile("Operator", UnlockLevel.L5_OPERATOR, true, Map.of(
                CapabilityFamily.EXTERNAL_WRITE, CapabilityDecision.REQUIRES_APPROVAL,
                CapabilityFamily.WORKSPACE_WRITE, CapabilityDecision.REQUIRES_APPROVAL,
                CapabilityFamily.SYSTEM_CONTROL, CapabilityDecision.REQUIRES_APPROVAL), OPEN_MATRIX);
    }

    /**
     * Unattended execution pinned at a ceiling, with no interactive escalation: approval-needing
     * families resolve to denied because nothing can approve them at runtime.
     */
    public static PermissionProfile automation(UnlockLevel ceiling) {
        return new PermissionProfile("Automation", ceiling, false, Map.of(
                CapabilityFamily.EXTERNAL_WRITE, CapabilityDecision.REQUIRES_APPROVAL,
                CapabilityFamily.WORKSPACE_WRITE, CapabilityDecision.REQUIRES_APPROVAL,
                CapabilityFamily.SYSTEM_CONTROL, CapabilityDecision.REQUIRES_APPROVAL), CLOSED_MATRIX);
    }

    private static ContentMatrix gradedMatrix() {
        EnumMap<ContentArea, ContentAllowance> map = new EnumMap<>(ContentArea.class);
        map.put(ContentArea.SEXUAL_EXPLICIT, ContentAllowance.PARTIAL);
        map.put(ContentArea.GRAPHIC_VIOLENCE, ContentAllowance.PARTIAL);
        map.put(ContentArea.STRONG_LANGUAGE, ContentAllowance.FULL);
        map.put(ContentArea.MATURE_THEMES, ContentAllowance.FULL);
        map.put(ContentArea.MEDICAL_EXPLICIT, ContentAllowance.FULL);
        return new ContentMatrix(map);
    }
}
