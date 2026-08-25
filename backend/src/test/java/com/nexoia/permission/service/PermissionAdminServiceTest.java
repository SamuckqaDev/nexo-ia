package com.nexoia.permission.service;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.nexoia.auth.user.model.UserRole;
import com.nexoia.permission.exception.PermissionDelegationDeniedException;
import com.nexoia.permission.model.ProfileKey;
import org.junit.jupiter.api.Test;

/**
 * Pins the delegation invariants: grant only at or below your own role and ceiling, never elevate
 * yourself, and never grant OWNER. OWNER is the unbounded grantor.
 */
class PermissionAdminServiceTest {

    private final PermissionAdminService service = new PermissionAdminService();

    @Test
    void ownerGrantsAnyRoleAndProfileBelowOwner() {
        assertThatCode(() -> service.assertCanGrant(
                UserRole.OWNER, ProfileKey.OPERATOR, UserRole.MEMBER, ProfileKey.OPERATOR))
                .doesNotThrowAnyException();
        assertThatCode(() -> service.assertCanGrant(
                UserRole.OWNER, ProfileKey.OPERATOR, UserRole.ADMIN, ProfileKey.BUILDER))
                .doesNotThrowAnyException();
    }

    @Test
    void ownerIsNeverGrantedByDelegation() {
        assertThatThrownBy(() -> service.assertCanGrant(
                UserRole.OWNER, ProfileKey.OPERATOR, UserRole.OWNER, ProfileKey.OPERATOR))
                .isInstanceOf(PermissionDelegationDeniedException.class);
    }

    @Test
    void adminCannotGrantAProfileAboveTheirOwnCeiling() {
        assertThatThrownBy(() -> service.assertCanGrant(
                UserRole.ADMIN, ProfileKey.RESEARCHER, UserRole.MEMBER, ProfileKey.OPERATOR))
                .isInstanceOf(PermissionDelegationDeniedException.class);
    }

    @Test
    void adminGrantsAProfileAtOrBelowTheirOwnCeiling() {
        assertThatCode(() -> service.assertCanGrant(
                UserRole.ADMIN, ProfileKey.BUILDER, UserRole.MEMBER, ProfileKey.RESEARCHER))
                .doesNotThrowAnyException();
    }

    @Test
    void nobodyGrantsARoleAtOrAboveTheirOwn() {
        // An admin cannot appoint another admin.
        assertThatThrownBy(() -> service.assertCanGrant(
                UserRole.ADMIN, ProfileKey.BUILDER, UserRole.ADMIN, ProfileKey.READER))
                .isInstanceOf(PermissionDelegationDeniedException.class);
        // A member cannot grant anything.
        assertThatThrownBy(() -> service.assertCanGrant(
                UserRole.MEMBER, ProfileKey.READER, UserRole.MEMBER, ProfileKey.LOCKED))
                .isInstanceOf(PermissionDelegationDeniedException.class);
    }
}
