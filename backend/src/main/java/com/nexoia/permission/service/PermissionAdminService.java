package com.nexoia.permission.service;

import com.nexoia.auth.user.model.UserRole;
import com.nexoia.permission.exception.PermissionDelegationDeniedException;
import com.nexoia.permission.model.BuiltInProfiles;
import com.nexoia.permission.model.ProfileKey;
import org.springframework.stereotype.Service;

/**
 * Enforces the delegation invariants of the governance model, deterministically: an actor may only grant
 * an authority and a capability profile at or below their own, and may never elevate themselves. OWNER
 * (root) is unbounded. It is the single authority for "who may grant what".
 *
 * <p>See {@code docs/ORGANIZATIONS_AND_GOVERNANCE.md} section 4. Authority is compared by an explicit
 * rank, never by {@code UserRole.ordinal()} (adding a role would silently shift ordinals). Capability
 * ceiling is compared by {@link com.nexoia.permission.model.UnlockLevel#ordinal()}, which is declared in
 * increasing order and is safe to compare.
 */
@Service
public class PermissionAdminService {

    /**
     * Verifies that {@code actor} may create or assign a principal with {@code targetRole} and
     * {@code targetProfile}. Throws {@link PermissionDelegationDeniedException} otherwise.
     *
     * <p>Rules, all fail-closed: OWNER is never granted by delegation (only bootstrap creates it); nobody
     * grants a role at or above their own (no self-elevation, no peer grant); and unless the actor is
     * OWNER (unbounded), the granted profile's capability ceiling may not exceed the actor's own.
     */
    public void assertCanGrant(UserRole actorRole, ProfileKey actorProfile,
            UserRole targetRole, ProfileKey targetProfile) {
        if (targetRole == UserRole.OWNER) {
            throw new PermissionDelegationDeniedException();
        }
        if (authorityRank(targetRole) >= authorityRank(actorRole)) {
            throw new PermissionDelegationDeniedException();
        }
        if (actorRole != UserRole.OWNER && ceilingRank(targetProfile) > ceilingRank(actorProfile)) {
            throw new PermissionDelegationDeniedException();
        }
    }

    private int authorityRank(UserRole role) {
        return switch (role) {
            case OWNER -> 3;
            case ADMIN -> 2;
            case MEMBER -> 1;
        };
    }

    private int ceilingRank(ProfileKey profile) {
        return BuiltInProfiles.of(profile).ceiling().ordinal();
    }
}
