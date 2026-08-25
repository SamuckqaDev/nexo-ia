package com.nexoia.permission.model;

import java.util.Map;

/**
 * A named, assignable permission preset: a ceiling {@link UnlockLevel} plus optional per-family
 * decision overrides. A family absent from {@code overrides} defaults to {@link CapabilityDecision#ALLOWED}
 * when the effective level and its target allow it; the {@link PermissionEngine} still clamps by level,
 * target, and prohibition.
 *
 * <p>{@code escalationAllowed} distinguishes an interactive profile from an unattended one: when it is
 * false (for example Automation), a family that would need approval is denied outright, because there is
 * no one to approve it. The {@code contentMatrix} carries the profile's per-area content policy — a
 * separate axis from capability: it never adds or removes a tool, and a capability level never restricts
 * a topic.
 */
public record PermissionProfile(
        String name,
        UnlockLevel ceiling,
        boolean escalationAllowed,
        Map<CapabilityFamily, CapabilityDecision> overrides,
        ContentMatrix contentMatrix) {

    public PermissionProfile {
        overrides = overrides == null ? Map.of() : Map.copyOf(overrides);
    }

    /** The profile's declared decision for a family, defaulting to ALLOWED when not overridden. */
    public CapabilityDecision baseDecision(CapabilityFamily family) {
        return overrides.getOrDefault(family, CapabilityDecision.ALLOWED);
    }
}
