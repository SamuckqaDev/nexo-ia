package com.nexoia.permission.dto;

import com.nexoia.permission.model.CapabilityDecision;
import com.nexoia.permission.model.CapabilityFamily;
import com.nexoia.permission.model.ContentStance;
import com.nexoia.permission.model.UnlockLevel;
import java.util.List;
import java.util.Map;

/**
 * The deterministic outcome of resolving one request against the Permission Engine: the effective
 * capability level, the decision for every {@link CapabilityFamily}, and the independent content stance.
 * It is the single source of truth the request assembly uses to attach tools and render the capability
 * envelope.
 */
public record ResolvedPermissions(
        UnlockLevel effectiveLevel,
        Map<CapabilityFamily, CapabilityDecision> decisions,
        ContentStance contentStance) {

    public ResolvedPermissions {
        decisions = decisions == null ? Map.of() : Map.copyOf(decisions);
    }

    public CapabilityDecision decision(CapabilityFamily family) {
        return decisions.getOrDefault(family, CapabilityDecision.DENIED);
    }

    public boolean isAllowed(CapabilityFamily family) {
        return decision(family) == CapabilityDecision.ALLOWED;
    }

    public boolean requiresApproval(CapabilityFamily family) {
        return decision(family) == CapabilityDecision.REQUIRES_APPROVAL;
    }

    /** Families that may run this request, directly or after approval — the attachable set. */
    public List<CapabilityFamily> attachable() {
        return decisions.entrySet().stream()
                .filter(entry -> entry.getValue() != CapabilityDecision.DENIED)
                .map(Map.Entry::getKey)
                .toList();
    }

    /** Families the model must be told are unavailable at this level. */
    public List<CapabilityFamily> locked() {
        return decisions.entrySet().stream()
                .filter(entry -> entry.getValue() == CapabilityDecision.DENIED)
                .map(Map.Entry::getKey)
                .toList();
    }
}
