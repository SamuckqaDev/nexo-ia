package com.nexoia.permission.service;

import com.nexoia.permission.dto.ResolvedPermissions;
import com.nexoia.permission.model.CapabilityDecision;
import com.nexoia.permission.model.CapabilityFamily;
import com.nexoia.permission.model.ContentStance;
import com.nexoia.permission.model.PermissionProfile;
import com.nexoia.permission.model.UnlockLevel;
import java.util.EnumMap;
import java.util.Set;
import org.springframework.stereotype.Service;

/**
 * Resolves, deterministically, what one request may do. It is the single choke point of the Permission
 * Engine: given the principal's profile, the mode ceiling, the selected model's tool capability, and the
 * targets already authorized outside the model, it returns the effective level and a decision per
 * capability family. Downstream code only enforces this result — it never re-decides.
 *
 * <p>The engine is pure and side-effect free so it can be exhaustively unit tested for axis isolation,
 * ceilings, hard prohibitions, and target gating. The content stance is passed through untouched: it is
 * an independent axis and must never influence which capabilities are attached.
 *
 * <p>See {@code docs/PERMISSION_PROFILES.md} section 8.
 */
@Service
public class PermissionEngine {

    /**
     * @param profile           the principal's assigned profile (ceiling plus per-family overrides)
     * @param modeCeiling       the workflow ceiling for the request's mode (for example Chat caps at L1)
     * @param modelSupportsTools whether the selected model can run the tool loop at all
     * @param authorizedTargets families whose external target is resolved and authorized for this
     *                          principal (a writable Vault, an enabled MCP connection, an attached
     *                          Workspace); families requiring a target are denied when absent
     * @param contentStance     the independent content axis, returned unchanged
     */
    public ResolvedPermissions resolve(
            PermissionProfile profile,
            UnlockLevel modeCeiling,
            boolean modelSupportsTools,
            Set<CapabilityFamily> authorizedTargets,
            ContentStance contentStance) {

        UnlockLevel effective = profile.ceiling().min(modeCeiling);
        if (!modelSupportsTools) {
            // A model that cannot call tools cannot run anything above grounded own-data capabilities.
            effective = effective.min(UnlockLevel.L1_GROUNDED);
        }

        EnumMap<CapabilityFamily, CapabilityDecision> decisions = new EnumMap<>(CapabilityFamily.class);
        for (CapabilityFamily family : CapabilityFamily.values()) {
            decisions.put(family, decide(family, profile, effective, authorizedTargets));
        }

        return new ResolvedPermissions(effective, decisions, contentStance);
    }

    private CapabilityDecision decide(
            CapabilityFamily family,
            PermissionProfile profile,
            UnlockLevel effective,
            Set<CapabilityFamily> authorizedTargets) {

        if (family.prohibited()) {
            return CapabilityDecision.DENIED;
        }
        if (!effective.grants(family.minLevel())) {
            return CapabilityDecision.DENIED;
        }
        if (family.requiresTarget() && !authorizedTargets.contains(family)) {
            return CapabilityDecision.DENIED;
        }

        CapabilityDecision decision = profile.baseDecision(family);
        if (decision == CapabilityDecision.REQUIRES_APPROVAL && !profile.escalationAllowed()) {
            // Nothing can approve at runtime (for example an unattended Automation profile).
            return CapabilityDecision.DENIED;
        }
        return decision;
    }
}
